import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mockdog.App
import mockdog.namesList
import mockdog.requests
import mockdog.server
import java.io.File
import java.net.InetAddress

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
    // when you want to connect android device(not emulator)
    // you need to add parameter: inetAddress = InetAddress.getByName("your local(pc/mac) ip address")
    server.start(inetAddress = InetAddress.getByName("192.168.1.107"), port = 52242)
  } catch (e: Throwable) {
    e.printStackTrace()
  }

  Window(
    title = "MockDog",
    onCloseRequest = ::exitApplication) {
    App(requests)
  }
}