package mockdog

import androidx.compose.runtime.*
import core.sendRealRequest
import okhttp3.*
import okhttp3.mockwebserver.*
import okhttp3.mockwebserver.Dispatcher
import java.net.InetAddress
import java.util.*
import java.util.concurrent.*

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Request(
  val id        : UUID,
  val request   : RecordedRequest,
  val body      : String,
  val timeStamp : Long = Date().time)

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
private var server: MockWebServer? = null

var servers = mutableStateListOf<ServerStatus>()
var errorDialog = mutableStateOf(Pair<Boolean,String>(false,""))


data class ServerStatus(var running: Boolean , var server: MockWebServer, val inetAddress: InetAddress)
// https://stackoverflow.com/questions/34037491/how-to-use-ssl-in-square-mockwebserver
fun startServer(port: Int = 52242, inetAddress: InetAddress = InetAddress.getByName("localhost")) {
  println(inetAddress.canonicalHostName)
  server?.shutdown()
  server = MockWebServer().apply {
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
    start(inetAddress, port)
  }
}



fun addAndStartServer(port: Int = 52242, inetAddress: InetAddress = InetAddress.getByName("localhost")) {
  var canAdd = true
  servers.forEach { serverStatus ->
    if(serverStatus.inetAddress.address.contentEquals(inetAddress.address)){
      errorDialog.value = Pair(true, "Ip Address already used")
      return

    }
  }

  val server = MockWebServer().apply {
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
    try {
      start(inetAddress, port)
    } catch (e: java.lang.Exception){
      errorDialog.value = Pair(true, e.message ?: "Unknown error")
      canAdd = false
    }
  }
  if(canAdd)
    servers.add(ServerStatus(true, server, inetAddress))
}

fun restartServer(hostName: String, port: Int): MockWebServer {
  val port = port + 1
  val newServer = MockWebServer().apply {
    dispatcher = object : Dispatcher() {
      override fun dispatch(recorded: RecordedRequest): MockResponse {
        val request = Request(
          id = UUID.randomUUID(),
          request = recorded,
          body = recorded.body.readUtf8()
        ) // TODO exception

        requests.add(request)

        val response: SentResponse = if (catchEnabled.value) {
          val value = queues.getOrPut(request.id) { ArrayBlockingQueue(1) }.take()
          if (value is SentResponse) value else {
            responses[request.id] = value; sendRealRequest(request)
          }
        } else
          sendRealRequest(request)

        responses[request.id] = response

        return MockResponse()
          .apply {
            if (bytesPerPeriod.value > 0 && timeInMillis.value > 0)
              throttleBody(bytesPerPeriod.value, timeInMillis.value, TimeUnit.MILLISECONDS)
          }
          .setResponseCode(response.status)
          .setHeaders(response.headers)
          .setBody(response.body)
      }
    }

    start(InetAddress.getByName(hostName), port)
  }
  return newServer
}

fun killAllServers(){
  servers.forEach {
    it.server?.shutdown()
    it.running = false
  }
}

fun sendMockResponse(id: UUID, code: Int, body: String) =
  queues[id]!!.put(SentResponse(url = "mock", status = code, body = body))

fun sendRealResponse(id: UUID) =
  queues[id]!!.put(Loading)