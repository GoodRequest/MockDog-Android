import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import core.initializeSavedMocks
import core.startServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.App

fun main() {
  startServer()

  application {
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { initializeSavedMocks() } }

    Window(
      title = "MockDog",
      state = rememberWindowState(size = DpSize(1920.dp, 1080.dp)),
      onCloseRequest = ::exitApplication
    ) { App() }
  }
}