package mockdog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import core.sendRealRequest
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import java.util.UUID
import java.util.concurrent.*

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Record(
  val id                : UUID,
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
private val responses = ConcurrentHashMap<UUID, BlockingQueue<SentResponse?>>()

fun serverLogic(server: MockWebServer) {
  // Dispatcher je riadne nestastny nazov. Je to len "Handler" ktory spracovava jeden request a synchronne vrati prislusny response
  server.dispatcher = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      // vytvori sa zaznam o prijatom requeste aj s informaciou ze sa procesuje na tomto vlakne
      val requestId = UUID.randomUUID()// = Thread.currentThread().id
      val record    = Record(id = requestId, request = request, wasReal = catchEnabled.value.not(), reqBody = request.body.readUtf8())
      requests.add(record)

      return if(catchEnabled.value) {
        // Cakam (blokujem toto vlakno) kym mi nepride z UI aku odpoved mam poslat
        responses.getOrPut(requestId) { ArrayBlockingQueue(1) }.take() ?: sendRealRequest(record)
      } else {
        sendRealRequest(record)
      }.also { response ->
        updateRecord(requestId) { copy(response = response) }
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

fun sendResponse(id: UUID, code: Int, body: String) =
  responses[id]!!.put(SentResponse(url = "mock response", status = code, body = body, headers = Headers.headersOf()))

fun sendRealShit(id: UUID) =
  responses[id]!!.put(null)

fun updateRecord(id: UUID, change: Record.() -> Record) {
  requests.indexOfFirst { it.id == id }.takeIf { it != -1 }?.let { index ->
    requests.set(index, requests[index].change())
  }
}