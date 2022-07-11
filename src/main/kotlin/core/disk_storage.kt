package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

private const val folder = "mocks"
private val mockFiles = mutableStateOf(emptyList<String>())

fun savedMocksFor(path: String) = mockFiles.value.filter {
  it.substringBeforeLast("_") == path
}

fun saveFile(path: String, name: String, body: String, dir: String = File("mocks/saveDir.txt").readText()) = try { // TODO nefunguje ak name v sebe obsahuje '_'
  val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
  File(dir).mkdirs()
  File("${dir}/$pathName.txt").writeText(body)
  mockFiles.value += pathName
} catch (e: Exception) {
  e.printStackTrace()
}

fun readFile(dir : String = File("mocks/saveDir.txt").readText(),name: String): String =
  try {
    File("${dir}/$name.txt").readText()
  } catch (e: Exception) {
    e.printStackTrace()
    "error reading file"
  }

fun loadMocks() =
  try {
    mockFiles.value = mockFiles.value + File(File("mocks/saveDir.txt").readText()).listFiles().map { it.name.substringBefore(".txt") }
  } catch (e: Throwable) {
    e.printStackTrace()
  }