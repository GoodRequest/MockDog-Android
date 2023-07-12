package core

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import ui.stringOrDefault
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// TODO tento file si zasluzi lasku

private val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

private val folder = "${System.getProperty("user.home")}${File.separatorChar}MockdogMocks".also { File(it).mkdirs() }
private val mockFiles = mutableStateOf(emptySet<String>())

val whiteListRequests = mutableStateListOf<String>()
fun saveFile(path: String, name: String, body: String, checkIfExist: Boolean = true): Boolean {
  return try {
    // TODO nefunguje ak name v sebe obsahuje '_'

    if (checkIfExist && getSavedMockFor(path).map { it.substringAfterLast("_").lowercase() }.contains(name.lowercase())) return true

    val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
    File(folder).mkdirs()
    File("${folder}/$pathName.txt").writeText(body)
    mockFiles.value += pathName
    false
  } catch (e: Exception) {
    e.printStackTrace()
    true
  }
}

fun deleteFile(name: String) = try {
  File("${folder}/$name.txt").delete()
  mockFiles.value -= name
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
  File("${folder}/$name.txt").readText().let { gson.stringOrDefault(it) }
} catch (e: Exception) {
  e.printStackTrace()
  null
}

fun saveWhiteList(
  requestLine: String,
  isAdd      : Boolean = true
) {
  val whiteListFolder = File(folder, "WhiteList")
  whiteListFolder.mkdir()

  if (isAdd) whiteListRequests.add(requestLine)
  else whiteListRequests.remove(requestLine)

  val outputJsonString = gson.toJson(whiteListRequests)
  File("${whiteListFolder}/actualWhiteList.json").writeText(outputJsonString)
}

fun readWhiteList() {
  runCatching {
    val whiteListJsonString = File("${folder}/WhiteList/actualWhiteList.json").readText()
    val whiteList = gson.fromJson<List<String>>(whiteListJsonString, object : TypeToken<List<String>>(){}.type)
    whiteListRequests.addAll(whiteList)
  }
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