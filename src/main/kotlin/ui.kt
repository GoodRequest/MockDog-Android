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
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
val namesList               = mutableStateOf(emptyList<String>())
val timeInMilis             = mutableStateOf<Long>(0)
val bytesPerPeriod          = mutableStateOf<Long>(0)

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

          ThrottleRequestSimulation(modifier = M.padding(horizontal = 16.dp).width(listWidth.value))
          //DelayRequestSimulation(modifier = M.padding(horizontal = 16.dp).width(listWidth.value))
          RequestHistory(requests)
        }

        // divider
        Box(M.width(4.dp)
          .fillMaxHeight()
          .background(Color.Black.copy(alpha = 0.2f))
          .draggable(dragula, Orientation.Horizontal))

        // detail
        selectedRequest.value?.let { index ->
          val (id, req, response, collapsedRequest, collapsedResponse, wasReal) = requests[index]

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

                    if (wasReal) {
                      DogTitleText(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text     = "You can save response to file:"
                      )

                      SaveMockItemsRow(
                        modifier   = Modifier.padding(horizontal = 16.dp),
                        body       = response.body,
                        setBody    = null,
                        setBodyErr = null,
                        path       = req.path!!
                      )
                    }
                  }
                else
                  ResponseForm(id, req.path!!, index)
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
fun ResponseForm(id: Long, path: String, index: Int) {
  val codes                 = remember { listOf(200, 401, 404, 500) }
  val (body, setBody)       = mutable("")
  val (bodyErr, setBodyErr) = mutable(false)

  Column(M.fillMaxWidth().padding(horizontal = 16.dp)) {

    Column {
      OutlinedTextField(
        modifier      = M.padding(bottom = 16.dp),
        maxLines      = 15,
        label         = { Text("Json mock", M.padding(top = 4.dp)) },
        colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
        isError       = bodyErr,
        value         = body,
        onValueChange = { setBody(it); setBodyErr(false) })

      if (bodyErr) { ErrorText("Zadaj json") }
    }

    // ukladanie  fake jsonu do suboru
    SaveMockItemsRow(body = body, setBody = setBody, setBodyErr = setBodyErr, path = path)

    DogTitleText(text = "Json mocks")

    // pomocny val kde mam ulozeny string requestu ale len cast pred query,
    // tento val nasledne pouzivame na porovnanie(vo filtri) s nazvami suborov ulozenych v namesList
    val requestPath = path.substringBefore("?").replace("/", "_").replace("?", "_")

    // zobrazenie prislunych file-ov ku danemu requestu v podobe tlacidiel s nazvom daneho file-u
    Row(M.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      namesList.value.filter {
        it.substringBeforeLast("_") == requestPath
      }.forEach { name ->
        Button(onClick = {
          setBody(readFile(name))
        }) {
          Text(modifier = M.padding(horizontal = 16.dp), text = name.substringAfterLast("_"))
        }
      }
    }

    Divider(
      modifier  = M.fillMaxWidth(),
      color     = MaterialTheme.colors.primary,
      thickness = 1.dp)

    DogTitleText(text = "Response codes")

    Row(M.padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
      codes.forEach { code ->
        Button(onClick  = { sendResponse(id, code, body) }) {
          Text(modifier = M.padding(horizontal = 16.dp), text = "$code")
        }
      }
      Button(onClick  = {
        sendRealShit(id)
        updateRecord(index) { copy(wasReal = true) }
      }) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "REAL SHIT")
      }
    }
  }
}

@Composable
fun SaveMockItemsRow(
  modifier  : Modifier = Modifier,
  body      : String,
  setBody   : ((String) -> Unit)?,
  setBodyErr: ((Boolean) -> Unit)?,
  path      : String
) {
  val (name, setName)       = mutable("")
  val (nameErr, setNameErr) = mutable(false)

  Row(
    modifier = modifier.padding(bottom = 16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalAlignment     = Alignment.Top
  ) {
    Column() {
      OutlinedTextField(
        maxLines      = 1,
        label         = { Text("Name of response", M.padding(top = 4.dp)) },
        colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
        isError       = nameErr,
        value         = name,
        onValueChange = { setName(it); setNameErr(false) })

      if(nameErr) { ErrorText("Zadaj názov súboru") }
    }

    Button(
      onClick = {
        when {
          name.isEmpty() || body.isEmpty() -> {
            if (name.isEmpty()) setNameErr(true)
            if (body.isEmpty() && setBodyErr != null) setBodyErr(true)
          }
          else -> {
            val pathName = path.substringBefore("?").replace("/", "_") + "_" + name
            saveFile(pathName, body)
            namesList.value = namesList.value + pathName
            setName("")
            if (setBody != null) {
              setBody("")
            }
          }
        }
      },
      modifier = M.padding(top = 10.dp)
    ) {
      Text(modifier = M.padding(horizontal = 16.dp), text = "SAVE")
    }
  }
}

@Composable
fun DogTitleText(modifier: Modifier = Modifier, text: String) {
  Text(
    modifier = modifier.padding(top = 16.dp),
    text     = text,
    style    = T.caption)
}

@Composable
fun ErrorText(text: String) {
  Text(
    text     = text,
    color    = C.error,
    style    = T.caption,
    modifier = M.padding(start = 16.dp, top = 4.dp))
}

@Composable
fun ThrottleRequestSimulation(modifier: Modifier) {
  ThrottleItem(modifier)
}

/*@Composable
fun DelayRequestSimulation(modifier: Modifier) {
  val (period, setPeriod) = mutable<Float?>(null)
  val time = period?.toLong()

  time?.let { timeInMilis.value = it }

  DogBodyText(modifier, "Delay request: ${if (time == null) "0 milis" else "$time milis"}")
  DogSlider(modifier, period, setPeriod)
}*/

@Composable
fun ThrottleItem(modifier: Modifier) {
  val (bytes, setBytes)   = mutable(0f)
  val (period, setPeriod) = mutable(0f)
  val data = bytes.toLong()
  val time = period.toLong()

  bytesPerPeriod.value = data
  timeInMilis.value    = time

  DogBodyText(modifier, "Bytes per period: ${if (data == 0L) "0 bytes" else "$data bytes"}")
  DogSlider(modifier, bytes, setBytes)

  DogBodyText(modifier, "Sleep after bytes: ${if (time == null) "0 milis" else "$time milis"}")
  DogSlider(modifier, period, setPeriod)
}

@Composable
fun DogSlider(modifier: Modifier, value: Float?, setValue: (Float) -> Unit) {
  Slider(
    modifier      = modifier,
    value         = value ?: 0.0f,
    onValueChange = setValue,
    valueRange    = 0f..100f,
    steps         = 99
  )
}

@Composable
fun DogBodyText(modifier: Modifier, text: String) {
  Text(
    modifier = modifier,
    style    = T.body2,
    text     = text
  )
}

@Composable fun <A> mutable(init: A) = remember { mutableStateOf(init) }
operator fun <A> Boolean.invoke(ifTrue: A, ifFalse: A) = if(this) ifTrue else ifFalse