package core

import androidx.compose.runtime.mutableStateOf
import java.io.File

private const val folder = "mocky"
private val mockFiles = mutableStateOf(emptyList<String>())

fun savedMocksFor(path: String) = mockFiles.value.filter {
  it.substringBeforeLast("_") == path
}

fun saveFile(name: String, body: String) {
  File(".\\${folder}\\$name.txt").writeText(body) // TODO try/catch
  mockFiles.value += name
}

fun readFile(name: String): String =
  File(".\\${folder}\\$name.txt").readText() // TODO try/catch

fun loadMocks() =
  try {
    mockFiles.value = mockFiles.value + File(".\\${folder}").listFiles().map { it.name.substringBefore(".txt") }
  } catch (e: Throwable) {
    e.printStackTrace()
  }