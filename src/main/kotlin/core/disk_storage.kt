package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

val namesList = mutableStateOf(emptyList<String>())

fun saveFile(name: String, body: String) {
  val file = File(".\\mocky\\$name.txt") // todo ukladanie na macu \/ ?
  file.writeText(body) // TODO try/catch
}

fun readFile(name: String): String {
  return File(".\\mocky\\$name.txt").readText() // TODO try/catch
}

fun loadMocks() {
  try {
    val path = ".\\mocky"
    val file = File(path)
    file.listFiles()?.forEach {// TODO map
      namesList.value = namesList.value + it.name.substringBefore(".txt")
    }
  } catch (e: Throwable) {
    e.printStackTrace()
  }
}