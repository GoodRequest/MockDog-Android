import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mockdog.App
import mockdog.requests
import mockdog.server

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