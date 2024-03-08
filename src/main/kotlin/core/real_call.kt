package core

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request as OkRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
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
        "DELETE"-> delete(body)
        else    -> post(body)
      }
    }
  ))

  val realResponse = measureTimedValue { runCatching(call::execute) }

  return realResponse.value.fold(
    onSuccess = { response ->
      SentResponse(
        url      = url.toString(),
        status   = response.code,
        duration = realResponse.duration.inWholeMilliseconds,
        headers  = Headers.Builder().apply { response.headers.filter { it.first.lowercase() != "content-encoding" }.forEach { add(it.first, it.second) } }.build(),
        body     = try {
          response.body!!.source().run {
            if(response.headers["content-encoding"]?.contains("gzip") == true)
              gzip()
            else
              this
          }.buffer().use(BufferedSource::readUtf8)
        } catch (e: Exception) {
          "Crash!\n" + e.printStackTrace()
        })
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