package core

import com.google.gson.*
import com.google.gson.stream.*
import java.io.Reader

inline fun json(block: JsonBuilder.() -> Unit): String =
  StringBuilder().apply { jsonBlock(block, ::append) }.toString()

fun parseJsonTree(json: Reader): Json =
  JsonRoot(JsonParser.read(JsonReader(json))!!)

private object JsonParser: TypeAdapter<Any?>() {
  override fun write(out: JsonWriter?, value: Any?) {}
  override fun read(reader: JsonReader): Any? {
    return when (reader.peek()) {
      JsonToken.STRING  -> reader.nextString()
      JsonToken.NUMBER  -> reader.nextString()
      JsonToken.BOOLEAN -> reader.nextBoolean()
      JsonToken.NULL    -> { reader.nextNull(); null }
      JsonToken.BEGIN_ARRAY -> {
        val array = ArrayList<Any?>()
        reader.beginArray()
        while (reader.hasNext()) { array.add(read(reader)) }
        reader.endArray()
        array
      }
      JsonToken.BEGIN_OBJECT -> {
        val obj = hashMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) { obj[reader.nextName()] = read(reader) }
        reader.endObject()
        obj
      }

      JsonToken.END_DOCUMENT,
      JsonToken.NAME,
      JsonToken.END_OBJECT,
      JsonToken.END_ARRAY, null -> throw IllegalArgumentException()
    }
  }
}

class JsonMismatch(message: String): RuntimeException(message)

sealed interface Json
private class JsonRoot(val element: Any): Json
private class JsonNest(val element: Any?, val parent: Json, val index: Int): Json
private class JsonPath(val parent: Json, val key: String): Json

operator fun Json.get(key: String): Json = JsonPath(this, key)

val Json.string : String     get() = nullString ?: fail()
val Json.int    : Int        get() = nullInt    ?: fail()
val Json.long   : Long       get() = nullLong   ?: fail()
val Json.bool   : Boolean    get() = nullBool   ?: fail()
val Json.double : Double     get() = nullDouble ?: fail()
val Json.list   : List<Json> get() = nullList   ?: fail()

val Json.nullString : String?     get() = parse { it as String? }
val Json.nullDouble : Double?     get() = parse { (it as String?)?.toDouble() }
val Json.nullInt    : Int?        get() = parse { (it as String?)?.toInt() }
val Json.nullBool   : Boolean?    get() = parse { it as Boolean? }
val Json.nullLong   : Long?       get() = parse { (it as String?)?.toLong() }
val Json.nullList   : List<Json>? get() = parse { it as List<Any?> }?.mapIndexed { i, v -> JsonNest(v, this, i) }

val Json.exists: Boolean get() = element != null
fun <A> Json.ifExists(action: (Json) -> A): A? = takeIf { exists }?.let(action)

val Json.nonEmptyList: List<Json> get() =
  nullList?.takeIf(List<Json>::isNotEmpty) ?: throw JsonMismatch("Failed to parse key '${path()}', list should not be empty }")

private inline fun <R> Json.parse(parse: (Any?) -> R): R? =
  try { parse(element) } catch(e: Exception) { null }

private fun Json.fail(): Nothing =
  throw JsonMismatch("Failed to parse key '${path()}', " + "actual value: ${element ?: "element not found"}")

private val Json.element: Any? get() = when(this) {
  is JsonPath -> (parent.element as? Map<*, *>)?.get(key)
  is JsonNest -> element
  is JsonRoot -> element
}

private tailrec fun Json.path(keys: List<String> = emptyList()): String = when(this) {
  is JsonPath -> parent.path(listOf(key) + keys)
  is JsonNest -> parent.path(listOf("[${index}]") + keys)
  is JsonRoot -> keys.joinToString("/")
}


// JSON Builder

class JsonBuilder(private val emit: (String) -> Unit) {
  private var first = true

  operator fun String.rangeTo(v: String?)  = append(if(v != null) "\"$v\"" else "null")
  operator fun String.rangeTo(v: Number?)  = append(v)
  operator fun String.rangeTo(v: Boolean?) = append(v)
  operator fun String.rangeTo(block: JsonBuilder.() -> Unit) { key(); jsonBlock(block, emit) }
  operator fun String.rangeTo(block: List<Any?>)             { key(); items(block) }

  fun json(block: JsonBuilder.() -> Unit): () -> Unit = { jsonBlock(block, emit) }

  private fun String.key() =
    emit(if (first.apply { first = false }) "\"$this\":" else ",\"$this\":")

  private fun String.append(value: Any?) =
    emit(if (first.apply { first = false }) "\"$this\":$value" else ",\"$this\":$value")

  private fun items(block: List<Any?>) {
    emit("[")
    block.forEachIndexed { i, v ->
      if(i != 0) emit(",")
      when (v) {
        is List<*>     -> items(v)
        is Function<*> -> (v as () -> Unit).invoke()
        is String      -> emit("\"${v}\"")
        else           -> emit(v.toString())
      }
    }
    emit("]")
  }
}

inline fun jsonBlock(block: JsonBuilder.() -> Unit, noinline emit: (String) -> Unit) {
  emit("{")
  JsonBuilder(emit).run(block)
  emit("}")
}