import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import core.loadMocks
import core.readFile
import core.saveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mockdog.App
import mockdog.addAndStartServer
import java.io.File
import java.net.InetAddress



fun main() {
  addAndStartServer(inetAddress = InetAddress.getLocalHost())
  try{
    File("mocks/saveDir.txt").readText()
  } catch (e: Exception){
    println(e.message)
    File("mocks").mkdirs()
    File("mocks/saveDir.txt").writeText("mocks")
    //saveFile("", "saveDir", "mocks")
  }

  application {
    LaunchedEffect(key1 = Unit) { withContext(Dispatchers.IO) { loadMocks() } }

    Window(
      title = "MockDog",
      state = rememberWindowState(size = DpSize(1920.dp, 1080.dp)),
      onCloseRequest = ::exitApplication
    ) {
      App()
    }
  }
}