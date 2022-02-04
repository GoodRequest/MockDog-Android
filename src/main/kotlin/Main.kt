// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import common.core.json
import okhttp3.OkHttp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import java.util.concurrent.ArrayBlockingQueue

data class Enrique(
  val request: RecordedRequest,
  val sent: Boolean)

val macka      = MockWebServer()
val requesty   = mutableStateListOf<Enrique>()
val blokovacka = ArrayBlockingQueue<MockResponse>(1)

fun main() = application {
  macka.dispatcher = object : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
      println("Requesto!")
      println(request.toString())
      val enro = Enrique(request, false)
      requesty.add(enro)

      println("dispo vlakno ${Thread.currentThread().id}")
      val response = blokovacka.take()
      val index = requesty.indexOf(enro)
      requesty[index] = enro.copy(sent = true)

      return response
    }
  }

  try {
    macka.start(port = 52242)
    println(macka.url("/"))
    println("asi bezim")
  } catch (e: Throwable) {
    e.printStackTrace()
  }

  Window(onCloseRequest = ::exitApplication) {
    App()
  }
}


@Composable
@Preview
fun App() {
  MaterialTheme {

    Column(modifier = Modifier.padding(16.dp)) {
      LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(requesty.toList()) { (req, sent) ->
          Column {
            Row {
              Text("Url: ", color = Color.DarkGray)
              Text(req.path!!)
            }

            Row {
              Text("Headers: ", color = Color.DarkGray)
              Text(req.headers.toString())
            }

            Row {
              Text("Body: ", color = Color.DarkGray)
              Text(req.body.toString())
            }

            if(sent)
              Text("JE TO TAM")
            else
              ŠupátkoNaspäť()

            Divider(modifier = Modifier.padding(top = 16.dp), color = Color.Black)
          }
        }
      }
    }
  }
}


@Composable
fun ŠupátkoNaspäť() {
  val (code, setCode) = mutableStateOf("200")
  val (body, setBody) = mutableStateOf(responza)

  Column(modifier = Modifier.width(500.dp)) {
    TextField(
      value = code,
      onValueChange = setCode
    )

    TextField(
      // modifier = Modifier.m
      maxLines = 20,
      value = body,
      onValueChange = setBody
    )

    Button(onClick = {
      blokovacka.put(
        MockResponse()
          .setResponseCode(code.toInt())
          .setBody(body)
      )
    }) {
      Text("Šupni")
    }
  }
}


val responza = """
    {
      "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4iLCJpYXQiOjE1MTYyMzkwMjJ9.A6Ak1IC1KhtSzAor4-i-bZhmCHQya-sRlPy9-DGgQwA",
        "profile": {
          "firstName": "string",
          "lastName": "string",
          "email": "user@example.com",
          "phone": "string",
          "yearOfBirth": "stri",
          "addressCity": "string",
          "points": 0,
          "cardID": "stringstringstri",
          "isAccountConfirmed": true,
          "recommendedProducts": [
            {
              "id": 1,
              "name": "Batoh Husky",
              "description": "Lorem ipsum dolor sit amet",
              "priceVariants": [
                {
                  "points": 23,
                  "price": 300
                }
              ],
              "gallery": [
                {
                  "url": "https://cdn.com/image1.png"
                }
              ],
              "category": {
                "name": "Šport"
              }
            }
          ]
        }
      },
      "messages": [
        {
          "type": "ERROR",
          "message": "string"
        }
      ]
    }
""".trimIndent()
