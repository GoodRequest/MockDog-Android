package core

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException

fun jsonBody(block: String) = object : RequestBody() {
  override fun contentType() = "application/json; charset=utf-8".toMediaTypeOrNull()
  override fun writeTo(sink: BufferedSink) { sink.writeUtf8(block) }
}

sealed interface ApiResponse {
  data class Content(val body: String): ApiResponse
  object     Loading                  : ApiResponse
}

fun POST(client: OkHttpClient, path: HttpUrl, body: RequestBody, headers: Headers = Headers.headersOf(), response: (ApiResponse) -> Unit) =
  client.execute(httpPost(path, body, headers), response)

private fun httpPost(url: HttpUrl, body: RequestBody, headers: Headers = Headers.headersOf()) =
  request(url, headers) { post(body) }

private fun request(url: HttpUrl, headers: Headers, config: Request.Builder.() -> Request.Builder) =
  Request.Builder().url(url).headers(headers).config().build()

private fun OkHttpClient.execute(request: Request, responseString: (ApiResponse) -> Unit): Unit =
  try {
    responseString(ApiResponse.Loading)
    newCall(request).enqueue(object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        responseString(ApiResponse.Content(e.localizedMessage))
      }

      override fun onResponse(call: Call, response: Response) {
        println(response)
        response.body?.string()?.let { responseString(ApiResponse.Content(it)) }
      }
    })
//    newCall(request).execute().use { response ->
//      response.body?.string()?.let { responseString(ApiResponse.Content(it)) }
//      println(response)
//    }
  } catch (e: Exception) {
    responseString(ApiResponse.Content(e.localizedMessage))
    e.printStackTrace()
  }
