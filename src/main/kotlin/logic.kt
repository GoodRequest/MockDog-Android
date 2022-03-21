package mockdog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.buffer
import okio.gzip
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

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

private val client = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
  .build()

//val realServerUrl = mutableStateOf("https://kia-mobile-dev.goodrequest.dev".toHttpUrlOrNull()!!)
//val realServerUrl = mutableStateOf("https://mobileapp.kia.sk/api/v1/")
//val realServerUrl = mutableStateOf("https://fitshaker-test.goodrequest.dev/")
val realServerUrl    = mutableStateOf("https://benzinol-dev.goodrequest.dev/")
val catchEnabled     = mutableStateOf(true)
val throttleCheckbox = mutableStateOf(false)
val deviceCheckbox   = mutableStateOf(false)

// Zoznam vsetkych prijatych requestov ktore prisli na server zoradeny podla dorucenia
val requests = mutableStateListOf<Record>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
private val responses = ConcurrentHashMap<Long, BlockingQueue<SentResponse>>()

fun serverLogic(server: MockWebServer) {
  server.apply {
    // Dispatcher je riadne nestastny nazov. Je to len "Handler" ktory spracovava jeden request a synchronne vrati prislusny response
    dispatcher = object : Dispatcher() {
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
          if (bytesPerPeriod.value > 0 && timeInMilis.value > 0) {
            MockResponse()
              //.setBodyDelay(timeInMilis.value ?: 0, TimeUnit.MILLISECONDS)
              .throttleBody(bytesPerPeriod.value, timeInMilis.value, TimeUnit.MILLISECONDS)
              .setResponseCode(it.status).setHeaders(it.headers).setBody(it.body)
          } else {
            MockResponse()
              //.setBodyDelay(timeInMilis.value, TimeUnit.MILLISECONDS)
              .setResponseCode(it.status).setHeaders(it.headers).setBody(it.body)
          }
        }
      }
    }
  }
}

private fun sendRealRequest(record: Record): SentResponse {
  val url = (realServerUrl.value.dropLastWhile { it == '/' } + record.request.requestUrl!!.encodedPath).toHttpUrlOrNull()!!
  val realResponse = runCatching {
    client.newCall(request(
      url     = url,
      headers = Headers.Builder().apply { record.request.headers.filter { it.first != "Host" }.forEach { add(it.first, it.second) } }.build(),
      config  = {
        val reqBody = record.reqBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()).apply { println("body: $this") }

        when(record.request.method.apply { println(this) }) {
          "GET"   -> get()
          "PUT"   -> put(reqBody)
          "PATCH" -> patch(reqBody)
          else    -> post(reqBody)
        }
      }
    )).execute()
  }

  return realResponse.fold(
    onSuccess = {

      SentResponse(
        url     = url.toString(),
        status  = it.code,
        headers = Headers.Builder().apply { it.headers.filter { it.first != "content-encoding" }.forEach { add(it.first, it.second) } }.build(),
        body    = it.body!!.source().run { if(it.headers["content-encoding"]?.contains("gzip") == true) gzip() else this }.buffer().readUtf8())
    },
    onFailure = {
      SentResponse(
        url     = url.toString(),
        status  = -1,
        headers = Headers.headersOf(),
        body    = it.stackTraceToString())
    })
}

private fun request(url: HttpUrl, headers: Headers, config: Request.Builder.() -> Request.Builder) =
  Request.Builder().url(url).headers(headers).config().build()

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

fun clearHistory() {
  requests.clear() // TODO zrusit neodoslane vlakna
  client.dispatcher.cancelAll()
  responses.clear()
}

fun saveFile(name: String, body: String) {
  val file = File(".\\mocky\\$name.txt") // todo ukladanie na macu \/ ?
  file.writeText(body)
}

fun readFile(name: String): String {
  return File(".\\mocky\\$name.txt").readText()
}