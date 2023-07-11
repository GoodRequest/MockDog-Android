package core

import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// TODO tento file si zasluzi lasku

private val folder = "${System.getProperty("user.home")}${File.separatorChar}MockdogMocks".also { File(it).mkdirs() }
val mockFiles = mutableStateOf(emptyList<String>())

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

fun saveUncaughtException(thread: Thread, throwable: Throwable) {
  val exceptionFolder = File(folder, "UncaughtExceptions")
  exceptionFolder.mkdir()

  val date = Date()

  val outputString = buildString {
    appendLine("Created: $date")
    appendLine()
    appendLine("Throwable StackTrace: ")
    appendLine(throwable.stackTraceToString())
    appendLine()
    appendLine("Thread StackTrace: ")
    appendLine(thread.stackTrace.joinToString("\n"))
  }

  File("${exceptionFolder}/${SimpleDateFormat("d-MMM-HH_mm").format(date)}.txt").writeText(outputString)
}