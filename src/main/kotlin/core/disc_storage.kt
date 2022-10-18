package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

// TODO tento file si zasluzi lasku

private val folder = "${System.getProperty("user.home")}${File.separatorChar}MockdogMocks".also { File(it).mkdirs() }
private val mockFiles = mutableStateOf(emptyList<String>())

fun saveFile(path: String, name: String, body: String) = try {
  // TODO nefunguje ak name v sebe obsahuje '_'
  val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
  File(folder).mkdirs()
  File("${folder}/$pathName.txt").writeText(body)
  mockFiles.value += pathName
} catch (e: Exception) {
  e.printStackTrace()
}

fun initializeSavedMocks() = try {
  mockFiles.value = mockFiles.value + File(folder).listFiles().map { it.name.substringBefore(".txt") }
} catch (e: Throwable) {
  e.printStackTrace()
}

fun getSavedMockFor(path: String): List<String> {
  val requestPath = path.substringBefore("?").replace("/", "_").replace("?", "_")
  return mockFiles.value.filter { it.substringBeforeLast("_") == requestPath }
}

fun readFile(name: String): String? = try {
  File("${folder}/$name.txt").readText()
} catch (e: Exception) {
  e.printStackTrace()
  null
}