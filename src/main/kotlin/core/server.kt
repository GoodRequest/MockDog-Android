package mockdog

import androidx.compose.runtime.*
import core.sendRealRequest
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import java.util.UUID
import java.util.concurrent.*

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Request(
  val id      : UUID,
  val request : RecordedRequest,
  val body    : String)

sealed interface Response
object Loading: Response
data class SentResponse(
  val url     : String  = "",
  val status  : Int     = 0,
  val headers : Headers = Headers.headersOf(),
  val body    : String  = "",
  val duration: Long    = 0): Response

val catchEnabled   = mutableStateOf(true)
val timeInMillis   = mutableStateOf<Long>(0)
val bytesPerPeriod = mutableStateOf<Long>(0)

// Zoznam vsetkych prijatych requestov ktore prisli na server zoradeny podla dorucenia
val requests  = mutableStateListOf<Request>()
val responses = mutableStateMapOf<UUID, Response>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
// TODO toto teraz rastie do nekonecna, mazat to ked uz je request odoslany
// TODO namiesto UUID by stacilo mat atomicky incrementovany Long
private val queues = ConcurrentHashMap<UUID, BlockingQueue<Response>>()

var server = MockWebServer().apply {
  dispatcher = object : Dispatcher() {
    override fun dispatch(recorded: RecordedRequest): MockResponse {
      val request = Request(
        id      = UUID.randomUUID(),
        request = recorded,
        body    = recorded.body.readUtf8()) // TODO exception

      requests.add(request)

      val response: SentResponse = if(catchEnabled.value) {
        val value = queues.getOrPut(request.id) { ArrayBlockingQueue(1) }.take()
        if(value is SentResponse) value else { responses[request.id] = value; sendRealRequest(request) }
      } else
        sendRealRequest(request)

      responses[request.id] = response

      return MockResponse()
        .apply { if (bytesPerPeriod.value > 0 && timeInMillis.value > 0)
          throttleBody(bytesPerPeriod.value, timeInMillis.value, TimeUnit.MILLISECONDS) }
        .setResponseCode(response.status)
        .setHeaders(response.headers)
        .setBody(response.body)
    }
  }
}

fun sendMockResponse(id: UUID, code: Int, body: String) =
  queues[id]!!.put(SentResponse(url = "mock", status = code, body = body))

fun sendRealResponse(id: UUID) =
  queues[id]!!.put(Loading)