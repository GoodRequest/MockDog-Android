package mockdog

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.sharp.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import theme.*
import ui.json.JsonTree

private val listWidth       = mutableStateOf(300.dp)
private val selectedRequest = mutableStateOf<Int?>(null)

@Composable
fun RequestHistory(requests: SnapshotStateList<Record>) {
  LazyColumn(M.background(Color.White), reverseLayout = true) {
    itemsIndexed(requests) { index, item ->
      val isSelected = (index == selectedRequest.value)
      Row(verticalAlignment = Alignment.CenterVertically, modifier = M
        .fillParentMaxWidth()
        .background(when {
          isSelected     -> BlueLight
          index % 2 == 1 -> C.surface
          else           -> C.background
        })
        .clickable { selectedRequest.value = index }
        .padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(M.width(40.dp)) {
          if (item.response == null)
            Icon(
              modifier = M.size(22.dp).clickable { sendRealShit(item.threadId) },
              imageVector         = Icons.Rounded.Send,
              tint                = C.onBackground,
              contentDescription  = "")
          else
            Text(
              text     = item.response?.status.toString(),
              color    = C.onBackground,
              style    = T.body2)
        }
        Text(
          modifier = M.width(50.dp),
          text     = item.request.method.toString(),
          color    = C.onBackground,
          style    = T.body2)
        Text(
          text  = item.request.path!!,
          style = T.body2)
      }
    }
  }
}

@Composable
fun App(requests: SnapshotStateList<Record>) {
  MaterialTheme(
    colors = ColorPalette,
    typography = TypographyTypes
  ) {
    val density = LocalDensity.current
    val dragula = rememberDraggableState { delta ->
      listWidth.value = listWidth.value + with(density) { delta.toDp() }
    }

    Surface(M.fillMaxWidth()) {

      Row {
        // lavy zoznam
        Column(M.width(listWidth.value).fillMaxHeight().background(Color.White)) {
        //  Button(onClick = { clearHistory() }) { Text("Clear") }
          BasicTextField(
            modifier = M.background(PrimeBlackVariant).padding(16.dp).fillMaxWidth(),
            textStyle = TextStyle.Default.copy(color = Color.White),
            value = realServerUrl.value,
            onValueChange = { realServerUrl.value = it })

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(modifier = M.padding(start = 16.dp), style = T.body2, text = "Mock responses")
            Checkbox(checked = catchEnabled.value, onCheckedChange = { catchEnabled.value = catchEnabled.value.not() })
          }
          RequestHistory(requests)
        }

        // divider
        Box(M.width(4.dp)
          .fillMaxHeight()
          .background(Color.Black.copy(alpha = 0.2f))
          .draggable(dragula, Orientation.Horizontal))

        // detail
        selectedRequest.value?.let { index ->
          val (id, req, response, collapsedRequest, collapsedResponse) = requests[index]

          Column(M.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(
              modifier = M.padding(bottom = 16.dp),
              text  = req.method + " " + req.path!!,
              style = T.subtitle1)

            // request
            Column(M.background(color = Color.White, shape = SH.medium).fillMaxWidth()) {
              Row(M.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  modifier = M
                    .size(30.dp)
                    .rotate(collapsedRequest(270f, 0f))
                    .clickable { updateRecord(index) { copy(collapsedRequest = !collapsedRequest) } },
                  imageVector         = Icons.Default.ArrowDropDown,
                  contentDescription  = "")
                Text("Request", style = T.caption)
              }

              if(collapsedRequest.not()) {
                HeadersTable(req.headers)
                Text(
                  modifier = M.padding(16.dp),
                  fontSize = 14.sp,
                  text     = req.body.toString())
              }
            }

            Box(M.height(16.dp))

            // response
            Column(M.background(color = Color.White, shape = SH.medium).fillMaxWidth()) {
              Row(M.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  modifier = M
                    .size(30.dp)
                    .rotate(collapsedResponse(270f, 0f))
                    .clickable { updateRecord(index) { copy(collapsedResponse = !collapsedResponse) } },
                  imageVector         = Icons.Default.ArrowDropDown,
                  contentDescription  = "")
                Text("Response", style = T.caption)
              }
              if(collapsedResponse.not()) {
                if (response != null)
                  Column {
                    Text(modifier = M.padding(horizontal = 16.dp), text = MockResponse().setResponseCode(response.status).status)
                    Text(modifier = M.padding(horizontal = 16.dp, vertical = 4.dp), text = response.url, style = T.body2, color = C.onSurface.copy(alpha = 0.6f))
                    HeadersTable(response.headers)

                    val (isFormatted, setFormatted) = remember { mutableStateOf(false) }

                    Row(M.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                      Text("Format JSON", fontSize = 12.sp, color = C.onSurface.copy(alpha = 0.6f))
                      Checkbox(checked = isFormatted, onCheckedChange = setFormatted)
                    }

                    if(isFormatted) {
                      val parsed = JsonParser.parseString(response.body)
                      val collapsed = remember { mutableStateListOf<String>() }
                      JsonTree(parsed, null, "", collapsed, {})
                    } else {
                      Text(
                        modifier = M.padding(16.dp),
                        fontSize = 14.sp,
                        text     = response.body)
                    }

                  }
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
fun HeadersTable(headers: Headers) {
  Row(M.padding(16.dp)) {
    Column {
      headers.forEach { (key, _) ->
        Text("$key  ", fontSize = 12.sp, color = C.onSurface.copy(alpha = 0.6f))
      }
    }
    Column {
      headers.forEach { (_, value) ->
        Text(
          text     = value,
          modifier = M.horizontalScroll(rememberScrollState()),
          fontSize = 12.sp,
          maxLines = 1)
      }
    }
  }
}

@Composable
fun ResponseForm(id: Long) {
  val codes = remember { listOf(200, 401, 404, 500) }
  val (body, setBody) = mutable(responza)

  Column(M.fillMaxWidth()) {

    TextField(
      maxLines      = 15,
      textStyle     = LocalTextStyle.current.copy(fontSize = 14.sp),
      value         = body,
      onValueChange = setBody)

    Row(M.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      codes.forEach { code ->
        Button(onClick = { sendResponse(id, code, body) }) {
          Text(modifier = M.padding(horizontal = 16.dp), text = "$code")
        }
      }
      Button(onClick = { sendRealShit(id) }) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "REAL SHIT")
      }
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
                  "point": 23,
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
