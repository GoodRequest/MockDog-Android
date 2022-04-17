import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import core.loadMocks
import mockdog.*

fun main() = application {
  LaunchedEffect(key1 = Unit) { loadMocks() }

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