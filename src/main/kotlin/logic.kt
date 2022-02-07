package mockdog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.nio.charset.Charset
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Record(
  val threadId          : Long,
  val request           : RecordedRequest,
  val response          : SentResponse? = null,
  val collapsedRequest  : Boolean = true,
  val collapsedResponse : Boolean = false)

data class SentResponse(
  val status  : Int,
  val headers : Headers,
  val body    : String)

private val client = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
  .build()

//val realServerUrl = mutableStateOf("https://kia-mobile-dev.goodrequest.dev".toHttpUrlOrNull()!!)
val realServerUrl = mutableStateOf("https://mobileapp.kia.sk/api/v1/")
val catchEnabled = mutableStateOf(true)

// Zoznam vsetkych prijatych requestov ktore prisli na server zoradeny podla dorucenia
private val requests = mutableStateListOf<Record>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
private val responses = ConcurrentHashMap<Long, BlockingQueue<SentResponse>>()

private val server = MockWebServer().apply {
  // Dispatcher je riadne nestastny nazov. Je to len "Handler" ktory spracovava jeden request a synchronne vrati prislusny response
  dispatcher = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      // vytvori sa zaznam o prijatom requeste aj s informaciou ze sa procesuje na tomto vlakne
      val threadId = Thread.currentThread().id
      val record   = Record(threadId, request)
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
          sendRealRequest(request)
        }
      } else {
        sendRealRequest(request)
      }.also { respo ->
        updateRecord(index) { copy(response = respo) }
      }.let {
        MockResponse().setResponseCode(it.status).setHeaders(it.headers).setBody(it.body)
      }
    }
  }
}

private fun sendRealRequest(request: RecordedRequest): SentResponse {
  val realResponse = runCatching {
    client.newCall(request(
      url = (realServerUrl.value.dropLast(1) + request.requestUrl!!.encodedPath).toHttpUrlOrNull()!!,
      headers = Headers.Builder().apply {
        request.headers.filter { it.first != "Host" }.forEach { add(it.first, it.second) }
      }.build(),
      config = {
        when(request.method.apply { println(this) }) {
          "GET" -> get()
          else -> post(request.body.readString(Charset.defaultCharset()).toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()).apply { println("body: $this") })
        }
      }
    )).execute()
  }

  return realResponse.fold(
    onSuccess = {
      SentResponse(
        status  = it.code,
        headers = it.headers,
        body    = it.body!!.string())
    },
    onFailure = {
      SentResponse(
        status  = -1,
        headers = Headers.headersOf(),
        body    = it.stackTraceToString())
    })
}

private fun request(url: HttpUrl, headers: Headers, config: Request.Builder.() -> Request.Builder) =
  Request.Builder().url(url).headers(headers).config().build()

fun sendResponse(id: Long, code: Int, body: String) {
  responses[id]!!.put(
    SentResponse(status = code, body = body, headers = Headers.headersOf()))
}

fun sendRealShit(id: Long) {
  responses[id]!!.put(SentResponse(status = 666, body = "", headers = Headers.headersOf()))
}

fun updateRecord(index: Int, change: Record.() -> Record) {
  requests[index] = requests[index].change()
}

fun clearHistory() {
  requests.clear() // TODO zrusit neodoslane vlakna
  client.dispatcher.cancelAll()
  responses.clear()
}

fun main() = application {
  try {
    server.start(port = 52242)
  } catch (e: Throwable) {
    e.printStackTrace()
  }

  Window(
    title = "MockDog",
    onCloseRequest = ::exitApplication) {
    App(requests)
  }
}