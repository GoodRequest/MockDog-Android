import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mockdog.*
import okhttp3.mockwebserver.MockWebServer
import java.io.File

var server = MockWebServer()

fun main() = application {
  // nacitanie ulozenych fake-json suborov do namesList uz pri starte - tym padom vieme hned zobrazovat ulozene subory
  LaunchedEffect(key1 = Unit) {
    try {
      val path = ".\\mocky"
      val file = File(path)
      file.listFiles()?.forEach {
        namesList.value = namesList.value + it.name.substringBefore(".txt")
      }
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  try {
    serverLogic(server)
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