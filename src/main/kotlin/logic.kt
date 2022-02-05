package mockdog

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ConcurrentHashMap

// "data" tu je iba kvoli copy metode. HashCode nefunguje dobre, bacha na to, nepouzivat!
data class Record(
  val threadId          : Long,
  val request           : RecordedRequest,
  val collapsedRequest  : Boolean = true,
  val collapsedResponse : Boolean = false,
  val sent              : Boolean = false)


// Zoznam vsetkych prijatych requestov ktore prisli na server zoradeny podla dorucenia
private val requests = mutableStateListOf<Record>()

// Kazdy prichadzajuci request sa procesuje na vlastnom samostatnom vlakne (toto handluje na pozadi okhttp).
// Toto vlakno caka (je blokovane) kym user na UI nezada data pre response.
// Takychto cakajucich vlakien moze byt viacero naraz. Blokovanie a zaroven aj preposielanie dat z UI vlakna
// je robene cez BlockingQueue. Tie sa tu drzia v mape kde kluc je ID vlakna ktore procesuje ten prislusny request
private val responses = ConcurrentHashMap<Long, BlockingQueue<MockResponse>>()

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

      // Cakam (blokujem toto vlakno) kym mi nepride z UI aku odpoved mam poslat
      val response = responses.getOrPut(threadId, { ArrayBlockingQueue(1) }).take()

      record.update(index) { copy(sent = true) }
      return response
    }
  }
}

fun sendResponse(id: Long, code: Int, body: String) {
  responses[id]!!.put(
    MockResponse()
      .setResponseCode(code)
      .setBody(body))
}

fun Record.update(index: Int, change: Record.() -> Record) {
  requests[index] = this.change()
}

fun main() = application {
  try {
    server.start(port = 52242)
  } catch (e: Throwable) {
    e.printStackTrace()
  }

  Window(onCloseRequest = {
    server.shutdown()
    exitApplication()
  }) {
    App(requests)
  }
}