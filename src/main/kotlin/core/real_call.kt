package core

import mockdog.*
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request as OkRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.buffer
import okio.gzip
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

private val client = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
  .build()

@OptIn(ExperimentalTime::class)
fun sendRealRequest(record: Request): SentResponse {
  val url = record.request.requestUrl!!.newBuilder().scheme("https").port(443).host(record.request.headers["Real-Host"]!!).build()
  val call = client.newCall(request(
    url     = url,
    headers = Headers.Builder().apply { record.request.headers.filter { it.first != "Host" }.forEach { add(it.first, it.second) } }.build(),
    config  = {
      val body = record.body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

      when(record.request.method) {
        "GET"   -> get()
        "PUT"   -> put(body)
        "PATCH" -> patch(body)
        else    -> post(body)
      }
    }
  ))

  val realResponse = measureTimedValue { runCatching(call::execute) }

  return realResponse.value.fold(
    onSuccess = {
      SentResponse(
        url      = url.toString(),
        status   = it.code,
        duration = realResponse.duration.inWholeMilliseconds,
        headers  = Headers.Builder().apply { it.headers.filter { it.first != "content-encoding" }.forEach { add(it.first, it.second) } }.build(),
        body     = it.body!!.source().run { if(it.headers["content-encoding"]?.contains("gzip") == true) gzip() else this }.buffer().readUtf8()) // TODO moze toto padnut? Closuje sa ten source?
    },
    onFailure = {
      SentResponse(
        url     = url.toString(),
        status  = -1,
        headers = Headers.headersOf(),
        body    = it.stackTraceToString())
    })
}

private fun request(url: HttpUrl, headers: Headers, config: OkRequest.Builder.() -> OkRequest.Builder) =
  OkRequest.Builder().url(url).headers(headers).config().build()