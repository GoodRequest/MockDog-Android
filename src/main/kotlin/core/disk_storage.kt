package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

private const val folder = "mocks"
private val mockFiles = mutableStateOf(emptyList<String>())

fun savedMocksFor(path: String) = mockFiles.value.filter {
  it.substringBeforeLast("_") == path
}

fun saveFile(path: String, name: String, body: String) { // TODO nefunguje ak name v sebe obsahuje '_'
  val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
  File(folder).mkdirs()
  File("${folder}/$pathName.txt").writeText(body) // TODO try/catch
  mockFiles.value += pathName
}

fun readFile(name: String): String =
  File("${folder}/$name.txt").readText() // TODO try/catch

fun loadMocks() =
  try {
    mockFiles.value = mockFiles.value + File(folder).listFiles().map { it.name.substringBefore(".txt") }
  } catch (e: Throwable) {
    e.printStackTrace()
  }