package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

private const val folder = "mocks"
private val mockFiles = mutableStateOf(emptyList<String>())

fun savedMocksFor(path: String) = mockFiles.value.filter {
  it.substringBeforeLast("_") == path
}

fun saveFile(path: String, name: String, body: String) = try { // TODO nefunguje ak name v sebe obsahuje '_'
  val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
  File(folder).mkdirs()
  File("${folder}/$pathName.txt").writeText(body)
  mockFiles.value += pathName
} catch (e: Exception) {
  e.printStackTrace()
}

fun readFile(name: String): String =
  try {
    File("${folder}/$name.txt").readText()
  } catch (e: Exception) {
    e.printStackTrace()
    "error reading file"
  }

fun loadMocks() =
  try {
    mockFiles.value = mockFiles.value + File(folder).listFiles().map { it.name.substringBefore(".txt") }
  } catch (e: Throwable) {
    e.printStackTrace()
  }