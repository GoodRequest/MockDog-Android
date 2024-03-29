
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import core.initializeSavedMocks
import core.saveUncaughtException
import core.startServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ui.App
import ui.showSearch

val windowState = WindowState(size = DpSize(1920.dp, 1080.dp))
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  startServer()

  application {
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { initializeSavedMocks() } }

    Window(
      title          = "MockDog",
      state          = windowState,
      onCloseRequest = ::exitApplication,
      onKeyEvent     = {
        if (it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyDown) {
          showSearch = showSearch.not()
          true
        } else false
      }
    ) { App() }

    // Registracia globalneho hanglera na neodchytene pady app
    Thread.setDefaultUncaughtExceptionHandler(::saveUncaughtException)
  }
}