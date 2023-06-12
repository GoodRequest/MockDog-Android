
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.hostOs
import ui.App
import ui.showSearch
import ui.showWhiteList

val windowState = WindowState(size = DpSize(1920.dp, 1080.dp))
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  startServer()

  application {

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { initializeSavedMocks(); readWhiteList() } }
    Window(
      title          = "MockDog",
      state          = windowState,
      onCloseRequest = ::exitApplication,
      onKeyEvent     = {
        if ((hostOs.isMacOS && it.isAltPressed) || it.isCtrlPressed && it.key == Key.F && it.type == KeyEventType.KeyDown) {
          showSearch = showSearch.not()
          true
        } else false
      }
    ) {
      MenuBar {
        Menu("Settings") {
          CheckboxItem(
            text            = "Show Search",
            shortcut        = if (hostOs.isMacOS) KeyShortcut(Key.F, alt = true) else KeyShortcut(Key.F, ctrl = true),
            checked         = showSearch,
            onCheckedChange = { showSearch = showSearch.not() }
          )

          if (whiteListRequests.isNotEmpty())
            Item("Show White list") { showWhiteList.value = true }
        }
      }

      App()
    }

    // Registracia globalneho hanglera na neodchytene pady app
    Thread.setDefaultUncaughtExceptionHandler(::saveUncaughtException)
  }
}