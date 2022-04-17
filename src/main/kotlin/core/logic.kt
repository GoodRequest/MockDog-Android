package mockdog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import core.sendRealRequest
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import java.util.concurrent.*

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Record(
  val threadId          : Long,
  val request           : RecordedRequest,
  val response          : SentResponse? = null,
  val collapsedRequest  : Boolean = true,
  val collapsedResponse : Boolean = false,
  val wasReal           : Boolean = false,
  val reqBody           : String)

data class SentResponse(
  val url     : String,
  val status  : Int,
  val headers : Headers,
  val body    : String)

var server = MockWebServer()

//val realServerUrl = mutableStateOf("https://kia-mobile-dev.goodrequest.dev".toHttpUrlOrNull()!!)
//val realServerUrl = mutableStateOf("https://mobileapp.kia.sk/api/v1/")
//val realServerUrl = mutableStateOf("https://fitshaker-test.goodrequest.dev/")
val realServerUrl    = mutableStateOf("https://benzinol-dev.goodrequest.dev/")
val catchEnabled     = mutableStateOf(true)
val throttleCheckbox = mutableStateOf(false)
val timeInMillis     = mutableStateOf<Long>(0)
val bytesPerPeriod   = mutableStateOf<Long>(0)
val deviceCheckbox   = mutableStateOf(false)


// Zoznam vsetkych prijatych requestov ktore prisli na server zoradeny podla dorucenia
val requests = mutableStateListOf<Record>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
private val responses = ConcurrentHashMap<Long, BlockingQueue<SentResponse>>()

fun serverLogic(server: MockWebServer) {
  // Dispatcher je riadne nestastny nazov. Je to len "Handler" ktory spracovava jeden request a synchronne vrati prislusny response
  server.dispatcher = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      // vytvori sa zaznam o prijatom requeste aj s informaciou ze sa procesuje na tomto vlakne
      val threadId = Thread.currentThread().id
      val record   = Record(threadId = threadId, request = request, wasReal = catchEnabled.value.not(), reqBody = request.body.readUtf8())
      requests.add(record)

      // Aby som zaznam vedel updatnut musim ho vediet neskor indetifikovat, tak beriem jeho index
      // BUG: Toto nemusi dat spravnu hodnotu ak parallelne stihol prebehnut iny zapis
      val index = requests.size - 1

      return if(catchEnabled.value) {
        // Cakam (blokujem toto vlakno) kym mi nepride z UI aku odpoved mam poslat
        val response = responses.getOrPut(threadId, { ArrayBlockingQueue(1) }).take()

        if(response.status != 666) {
          response
        } else {
          sendRealRequest(record)
        }
      } else {
        sendRealRequest(record)
      }.also { respo ->
        updateRecord(index) { copy(response = respo) }
      }.let {
        if (bytesPerPeriod.value > 0 && timeInMillis.value > 0) {
          MockResponse()
            .throttleBody(bytesPerPeriod.value, timeInMillis.value, TimeUnit.MILLISECONDS)
            .setResponseCode(it.status).setHeaders(it.headers).setBody(it.body)
        } else {
          MockResponse()
            .setResponseCode(it.status).setHeaders(it.headers).setBody(it.body)
        }
      }
    }
  }
}

fun sendResponse(id: Long, code: Int, body: String) {
  responses[id]!!.put(
    SentResponse(url = "mock response", status = code, body = body, headers = Headers.headersOf()))
}

fun sendRealShit(id: Long) {
  val url = realServerUrl.value.dropLastWhile { it == '/' }
  responses[id]!!.put(SentResponse(url = url, status = 666, body = "", headers = Headers.headersOf()))
}

fun updateRecord(index: Int, change: Record.() -> Record) {
  requests[index] = requests[index].change()
}