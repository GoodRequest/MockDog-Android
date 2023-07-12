package core

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import okhttp3.Headers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val requests  = mutableStateListOf<Request>()
val responses = mutableStateMapOf<UUID, Response>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
// TODO toto teraz rastie do nekonecna, mazat to ked uz je request odoslany
// TODO namiesto UUID by stacilo mat atomicky incrementovany Long
private val queues = ConcurrentHashMap<UUID, BlockingQueue<Response>>()
private var server : MockWebServer? = null

val mockingEnabled   = mutableStateOf(true)
val mockRealResponse = mutableStateOf(true)
val throttle         = mutableStateOf(Throttle(isEnabled = false, delay = 0))
val useDevice        = mutableStateOf(true)

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Request(
  val id      : UUID,
  val created : Long,
  val request : RecordedRequest,
  val body    : String)

sealed interface Response
object Loading : Response
data class EditResponse(val response: SentResponse): Response
data class SentResponse(
  val url      : String  = "",
  val status   : Int     = 0,
  val headers  : Headers = Headers.headersOf(),
  val body     : String  = "",
  val duration : Long    = 0) : Response

// https://stackoverflow.com/questions/34037491/how-to-use-ssl-in-square-mockwebserver
fun startServer(port: Int = 52242, inetAddress: InetAddress = getDefaultIpV4Address()) {
  server?.shutdown()
  server = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(recorded: RecordedRequest): MockResponse {
        val request = Request(
          id      = UUID.randomUUID(),
          created = System.currentTimeMillis(),
          request = recorded,
          body    = recorded.body.readUtf8()) // TODO exception

        requests.add(request)

        val response: SentResponse = if(mockingEnabled.value && whiteListRequests.contains(recorded.requestLine).not()) {
          if (mockRealResponse.value) {
            responses[request.id] = Loading
            responses[request.id] = EditResponse(sendRealRequest(request))
          }
          val value = queues.getOrPut(request.id) { ArrayBlockingQueue(1) }.take()
          if(value is SentResponse) {
            value
          } else {
            responses[request.id] = value
            sendRealRequest(request)
          }
        } else {
          sendRealRequest(request)
        }

        responses[request.id] = response

        return MockResponse()
          .apply {
            if (throttle.value.isEnabled) throttleBody(10, throttle.value.delay, TimeUnit.MILLISECONDS)
          }
          .setResponseCode(response.status)
          .setHeaders(response.headers)
          .setChunkedBody(response.body, 25000)
      }
    }
    start(inetAddress, port)
  }
}

private fun getDefaultIpV4Address() = NetworkInterface.getNetworkInterfaces()
  .toList()
  .flatMap { it.inetAddresses.toList() }
  .filterIsInstance<Inet4Address>()
  .firstOrNull { it.hostAddress != "127.0.0.1" } ?: Inet4Address.getByName("127.0.0.1")

data class Throttle(val isEnabled: Boolean, val delay: Long)

fun sendMockResponse(id: UUID, url: String?, code: Int, body: String) =
  queues[id]!!.put(SentResponse(url = url ?: "Mock - Unknown url", status = code, body = body))

fun sendRealResponse(id: UUID) =
  queues[id]!!.put(Loading)

fun sendRealResponseAll() = requests.forEach { request ->
  if (responses.contains(request.id).not() || responses[request.id] is EditResponse) {
    queues[request.id]!!.put(Loading)
  }
}

fun restartServer(useRealDevice: Boolean) {
  sendRealResponseAll()
  server?.close()
  startServer(inetAddress = if (useRealDevice) getDefaultIpV4Address() else InetAddress.getByName("localhost"))
}