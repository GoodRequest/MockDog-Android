package core

import mockdog.*
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.buffer
import okio.gzip

private val client = OkHttpClient.Builder()
  .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
  .build()

fun sendRealRequest(record: Record): SentResponse {
  val url = record.request.requestUrl!!.newBuilder().scheme("https").port(443).host(record.request.headers["Real-Host"]!!).build()
  val realResponse = runCatching {
    client.newCall(request(
      url     = url,
      headers = Headers.Builder().apply { record.request.headers.filter { it.first != "Host" }.forEach { add(it.first, it.second) } }.build(),
      config  = {
        val reqBody = record.reqBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()).apply { println("body: $this") }

        when(record.request.method.apply { println(this) }) {
          "GET"   -> get()
          "PUT"   -> put(reqBody)
          "PATCH" -> patch(reqBody)
          else    -> post(reqBody)
        }
      }
    )).execute()
  }

  return realResponse.fold(
    onSuccess = {
      SentResponse(
        url     = url.toString(),
        status  = it.code,
        headers = Headers.Builder().apply { it.headers.filter { it.first != "content-encoding" }.forEach { add(it.first, it.second) } }.build(),
        body    = it.body!!.source().run { if(it.headers["content-encoding"]?.contains("gzip") == true) gzip() else this }.buffer().readUtf8())
    },
    onFailure = {
      SentResponse(
        url     = url.toString(),
        status  = -1,
        headers = Headers.headersOf(),
        body    = it.stackTraceToString())
    })
}

private fun request(url: HttpUrl, headers: Headers, config: Request.Builder.() -> Request.Builder) =
  Request.Builder().url(url).headers(headers).config().build()