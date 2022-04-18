import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import core.loadMocks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mockdog.*

fun main() {
  server.start(port = 52242)

  application {
    LaunchedEffect(key1 = Unit) { withContext(Dispatchers.IO) { loadMocks() } }

    Window(
      title = "MockDog",
      onCloseRequest = ::exitApplication
    ) {
      App()
    }
  }
}