package mockdog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import theme.*


@Composable
@Preview
fun App(requests: SnapshotStateList<Record>) {
  MaterialTheme(
    colors = ColorPalette,
    typography = TypographyTypes
  ) {
    Surface(M.fillMaxWidth().padding(16.dp)) {
      if(requests.isEmpty())
        Text("Waiting for first request...")
      else
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(requests) { index, item ->
          val (id, req, collapsedRequest, collapsedResponse, sent) = item

          Column {
            Text(
              text  = req.method + " " + req.path!!,
              style = T.subtitle1)

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    modifier = M
                      .size(30.dp)
                      .rotate(collapsedRequest(270f, 0f))
                      .clickable { item.update(index) { copy(collapsedRequest = !collapsedRequest) } },
                    imageVector         = Icons.Default.ArrowDropDown,
                    contentDescription  = "")
                  Text("Request", style = T.caption)
                }

                if(collapsedRequest.not()) {
                  Row(M.padding(vertical = 16.dp)) {
                    Column {
                      req.headers.forEach { (key, _) ->
                        Text("$key  ", fontSize = 12.sp, color = C.onSurface.copy(alpha = 0.6f))
                      }
                    }
                    Column {
                      req.headers.forEach { (_, value) ->
                        Text(
                          text     = value,
                          modifier = M.horizontalScroll(rememberScrollState()),
                          fontSize = 12.sp,
                          maxLines = 1)
                      }
                    }
                  }
                  Text(
                    modifier = M.padding(bottom = 16.dp),
                    fontSize = 14.sp,
                    text     = req.body.toString())
                }
              }

            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  modifier = M
                    .size(30.dp)
                    .rotate(collapsedResponse(270f, 0f))
                    .clickable { item.update(index) { copy(collapsedResponse = !collapsedResponse) } },
                  imageVector         = Icons.Default.ArrowDropDown,
                  contentDescription  = "")
                Text("Response", style = T.caption)
              }
              if(collapsedResponse.not()) {
                if (sent)
                  Text("RESPONSE SENT")
                else
                  ResponseForm(id)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun ResponseForm(id: Long) {
  val codes           = remember { listOf(200, 401, 500) }
  val (code, setCode) = mutable(200)
  val (body, setBody) = mutable(responza)

  Column(M.fillMaxWidth()) {

    Row(M.padding(vertical = 16.dp)) {
      codes.forEach {
        Text(
          text     = it.toString(),
          color    = (it == code)(C.surface, C.primaryVariant),
          modifier = Modifier
          .background(
            color = (it == code)(C.primaryVariant, C.surface),
            shape = RoundedCornerShape(12.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
          .clickable { setCode(it) })
      }
    }

    TextField(
      maxLines      = 15,
      textStyle     = LocalTextStyle.current.copy(fontSize = 14.sp),
      value         = body,
      onValueChange = setBody)

    Button(onClick = { sendResponse(id, code, body) }) {
      Text(modifier = M.padding(horizontal = 32.dp), text = "SEND")
    }
  }
}

@Composable fun <A> mutable(init: A) = remember { mutableStateOf(init) }
operator fun <A> Boolean.invoke(ifTrue: A, ifFalse: A) = if(this) ifTrue else ifFalse

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
