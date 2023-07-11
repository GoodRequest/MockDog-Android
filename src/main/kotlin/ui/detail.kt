package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import core.*
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.text.SimpleDateFormat
import java.util.*


val prettyGson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
var showSearch       by mutableStateOf(false)
@Composable
fun Detail(id : UUID) {
  val isExpandedRequest  = mutable(false)
  val isExpandedResponse = mutable(true)

  val selectedRequest = requests.firstOrNull { it.id == id }
  val response        = responses[id]
  val scrollState     = rememberScrollState()
  val editResponse    = remember(response) { response as? EditResponse }

  val (responseBody, setResponseBody) = remember(id) {
    mutableStateOf<ResponseType>(ResponseType.Real(editResponse?.response?.body ?: ""))
  }

  selectedRequest?.let {
    Column(M.fillMaxSize().padding(16.dp)) {
      Box(M.weight(1f).padding(bottom = 4.dp)) {
        Column(
          modifier = M.verticalScroll(scrollState),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          Text(
            modifier = M.padding(bottom = 8.dp),
            text     = listOfNotNull(it.request.method, it.request.path).joinToString(" "),
            style    = T.subtitle1)
          RequestTime(it.created)
          DetailRequest(
            headers     = it.request.headers,
            body        = it.body,
            isExpanded  = isExpandedRequest.value,
            onClick     = { isExpandedRequest.value = isExpandedRequest.value.not() })
          DetailResponse(
            request         = it.request,
            response        = response,
            isExpanded      = isExpandedResponse.value,
            responseBody    = responseBody,
            setResponseBody = setResponseBody,
            onClick         = { isExpandedResponse.value = isExpandedResponse.value.not() })
        }
        VerticalScrollbar(
          adapter  = rememberScrollbarAdapter(scrollState),
          modifier = M.align(Alignment.CenterEnd).fillMaxHeight()
        )
      }

      AnimatedVisibility(isExpandedResponse.value && response !is SentResponse && response !is Loading) {
        Divider(M.height(1.dp))

        // Response options
        ResponseOptions(
          id              = id,
          requestPath     = it.request.path,
          editResponse    = editResponse,
          responseBody    = responseBody,
          setResponseBody = setResponseBody
        )
      }
    }
  }
}

@Composable
private fun ResponseOptions(
  id             : UUID,
  requestPath    : String?,
  editResponse   : EditResponse?,
  responseBody   : ResponseType,
  setResponseBody: (ResponseType) -> Unit
) {
  val codesExpanded = mutable(false)
  val codes         = derivedStateOf { if (codesExpanded.value) allHttpCodes else httpCodes }
  val savedMocks    = getSavedMockFor(requestPath ?: "Unknown path")

  Column {
    // Response codes
    ContextMenuArea(items = { listOf(ContextMenuItem(if (codesExpanded.value) "Collapse" else "Expand") { codesExpanded.value = codesExpanded.value.not() }) }) {
      Row(modifier = M.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

        //Real response
        Button(
          modifier = M.padding(end = 16.dp),
          onClick  = { sendRealResponse(id) },
          shape    = CircleShape
        ) {
          Text(modifier = M.padding(horizontal = 16.dp), text = "Real")
        }

        // HTTP codes
        codes.value.forEach { entry ->
          Button(onClick  = { sendMockResponse(id = id, url = editResponse?.response?.url, code = entry.key, body = responseBody.value) }, shape = CircleShape) {
            Text(modifier = M.padding(horizontal = 8.dp), text = if (codesExpanded.value) "${entry.key} - ${entry.value}" else "${entry.key}")
          }
        }
      }
    }

    // Saved mocks
    if(savedMocks.isNotEmpty()) {
      Text(modifier = M.padding(bottom = 4.dp), text = "Load saved mocks", style = T.caption)
      Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        savedMocks.forEach { file ->
          val name = file.substringAfterLast("_")
          ContextMenuArea(items = { listOf(ContextMenuItem("Delete saved mock") { deleteFile(file) }) }) {
            Button(
              onClick = { setResponseBody(ResponseType.Mock(mockBody = readFile(file) ?: "Cannot read file",path = file, name = name)) },
              shape   = CircleShape) {
              Text(modifier = M.padding(horizontal = 8.dp), text = name)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RequestTime(created: Long) {
  Column(M.padding(bottom = 8.dp)) {
    val milliseconds = System.currentTimeMillis() - created
    val seconds      = (milliseconds / 1000) % 60
    val minutes      = (milliseconds / (1000 * 60) % 60)
    val hours        = (milliseconds / (1000 * 60 * 60) % 24)

    Text("Request created: ${SimpleDateFormat("d MMM, HH:mm:ss").format(Date(created))}")
    Text("Created ${if (hours > 0) "${hours}h " else ""}${if (minutes > 0) "${minutes}min" else ""} ${seconds}sec ago")
  }
}

@Composable
private fun DetailRequest(
  modifier   : Modifier = Modifier,
  headers    : Headers,
  body       : String,
  isExpanded : Boolean,
  onClick    : () -> Unit
) {
  var prettyPrintJson by mutable(true)
  var isFormatted     by mutable(true)

  Surface(modifier = modifier.fillMaxWidth()) {
    Column {
      CollapseHeader(
        text       = "Request",
        isExpanded = isExpanded,
        onClick    = onClick)
      AnimatedVisibility(isExpanded) {
        Column {
          HeadersTable(modifier = M.padding(16.dp), headers = headers)
          if (body.isNotBlank()) {

            val menuItems = mutableListOf(
              ContextMenuItem(if (isFormatted) "Show Json" else "Format Json") { isFormatted = isFormatted.not() },
              ContextMenuItem(if (showSearch )"Hide search bar" else "Show search bar") { showSearch = !showSearch })

            if (isFormatted.not()) menuItems.add(0, (ContextMenuItem(if (prettyPrintJson) "Raw Json" else "Pretty print Json") { prettyPrintJson = prettyPrintJson.not() }))

            // Json tree
            ContextMenuDataProvider(
              items = { menuItems }
            ) { JsonArea(
              isFormatted   = isFormatted,
              body          = body,
              isPrettyPrint = prettyPrintJson)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DetailResponse(
  modifier       : Modifier = Modifier,
  request        : RecordedRequest,
  response       : Response?,
  isExpanded     : Boolean,
  responseBody   : ResponseType,
  setResponseBody: (ResponseType) -> Unit,
  onClick        : () -> Unit
) {
  val isSaveMockDialogVisible = mutable(false)

  Surface(modifier = modifier.fillMaxWidth()) {
    Column {
      CollapseHeader(
        text       = "Response",
        isExpanded = isExpanded,
        onClick    = onClick)
      AnimatedVisibility(isExpanded) {
        when(response) {
          null, is EditResponse -> ResponseForm(body = responseBody, setBody = setResponseBody, requestPath = request.path)
          is Loading            -> Text(modifier = M.padding(16.dp), text = "Sending...")
          is SentResponse       -> {
            var prettyPrintJson    by mutable(true)
            var isFormatted        by mutable(true)
            var areHeadersExpanded by mutable(false)

            // Main info
            Column {
              Text(
                modifier = M.padding(horizontal = 16.dp),
                text     = MockResponse().setResponseCode(response.status).status + " (${response.duration}ms)")
              Text(
                modifier = M.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                text     = response.url,
                style    = T.body2,
                color    = C.onSurface.copy(alpha = 0.6f))

              // Headers
              if (response.headers.size > 0) {
                CollapseHeader(
                  text       = "Headers",
                  isExpanded = areHeadersExpanded,
                  onClick    = { areHeadersExpanded = areHeadersExpanded.not() })
                AnimatedVisibility(areHeadersExpanded) {
                  HeadersTable(modifier = M.padding(16.dp), headers = response.headers)
                }
              }

              if (response.body.isNotBlank()) {

                val menuItems = mutableListOf(
                  ContextMenuItem(if (isFormatted) "Show Json" else "Format Json") { isFormatted = isFormatted.not() },
                  ContextMenuItem("Save to file") { isSaveMockDialogVisible.value = true },
                  ContextMenuItem(if (showSearch )"Hide search bar" else "Show search bar") { showSearch = !showSearch })

                if (isFormatted.not()) menuItems.add(0, (ContextMenuItem(if (prettyPrintJson) "Raw Json" else "Pretty print Json") { prettyPrintJson = prettyPrintJson.not() }))

                // Json tree
                ContextMenuDataProvider(
                  items = { menuItems }
                ) { JsonArea(
                  isFormatted   = isFormatted,
                  body          = response.body,
                  isPrettyPrint = prettyPrintJson) }
              }
            }
          }
        }
      }
    }
  }

  if (isSaveMockDialogVisible.value && response is SentResponse) {
    SaveMockDialog(
      onCloseRequest = { isSaveMockDialogVisible.value = false },
      requestPath    = request.path,
      responseBody   = response.body)
  }
}

@Composable
fun SaveMockDialog(
  onCloseRequest: () -> Unit,
  requestPath   : String?,
  responseBody  : String,
) {
  val isMockNameTheSame = mutable(false)

  Dialog(
    state          = rememberDialogState(size = DpSize(400.dp, 200.dp)),
    onCloseRequest = onCloseRequest,
    title          = "Save mock to file",
    resizable      = false,
    content        = {
      val name = mutable("")

      Column (
        modifier = Modifier.padding(16.dp)
      ) {
        OutlinedTextField(
          modifier      = M.fillMaxWidth(),
          maxLines      = 1,
          label         = { Text("Name of response", style = T.caption) },
          singleLine    = true,
          value         = name.value,
          onValueChange = { name.value = it; isMockNameTheSame.value = false },
          isError       = isMockNameTheSame.value,
          trailingIcon  = {
            if (isMockNameTheSame.value)
              Icon(Icons.Filled.Warning,"error", tint = MaterialTheme.colors.error)
          })
        if (isMockNameTheSame.value) {
          Text(
            text     = "Response name is already exists, try another one.",
            color    = MaterialTheme.colors.error,
            style    = MaterialTheme.typography.caption,
            modifier = M.padding(start = 16.dp)
          )
        }
        Spacer(modifier = M.weight(1f))
        Button(
          modifier = M.align(Alignment.End),
          enabled  = name.value.isNotBlank() && isMockNameTheSame.value.not(),
          onClick = {
            isMockNameTheSame.value = saveFile(requestPath ?: "Unknown path", name.value, responseBody)
            if (isMockNameTheSame.value.not()) onCloseRequest()
          }
        ) {
          Text(modifier = M.padding(horizontal = 16.dp), text = "Save")
        }
      }
    })
}

@Composable
fun ResponseForm(body: ResponseType, setBody: (ResponseType) -> Unit, requestPath: String?) {
  var isSaveMockDialogVisible by mutable(false)
  var prettyPrintJson         by mutable(true)

  val menuItems = listOf(
    ContextMenuItem(if (prettyPrintJson) "Raw Json" else "Pretty print Json") { prettyPrintJson = prettyPrintJson.not() },
    ContextMenuItem("Save to new mock file") { isSaveMockDialogVisible = true },
    if (body is ResponseType.Mock) ContextMenuItem("Edit actual mock file (${body.name})") {
      saveFile(path = requestPath ?: "Unknown path", name = body.name, body = body.value, checkIfExist = false)
    } else null
  ).mapNotNull { it }

  Column(modifier = M.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
    // Insert own JSON
    // TODO add search

    ContextMenuDataProvider(items = { menuItems }) {
      EditableTextWithSearch(
        modifier      = M.fillMaxWidth().padding(bottom = 16.dp),
        label         = { Text("JSON mock", style = T.caption) },
        colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
        value         = if (prettyPrintJson) prettyGson.stringOrDefault(body.value) else body.value,
        onValueChange = { changedBody ->
          when(body) {
            is ResponseType.Mock -> setBody(ResponseType.Mock(mockBody = changedBody, path = body.path, name = body.name))
            is ResponseType.Real -> setBody(ResponseType.Real(realBody = changedBody))
          }
        })
    }
  }

  if (isSaveMockDialogVisible)
    SaveMockDialog(
      onCloseRequest = { isSaveMockDialogVisible = false },
      requestPath    = requestPath,
      responseBody   = body.value)
}

@Composable
private fun JsonArea(
  isFormatted   : Boolean,
  isPrettyPrint : Boolean,
  body          : String
) {
  val parsed = try { JsonParser.parseString(body) } catch (e: Exception) {
    e.printStackTrace()
    JsonParser.parseString("{}")
  }

  Box(modifier = M.padding(vertical = 16.dp)) {
    if(isFormatted)
      MocDogJsonParser(element = parsed, showSearch = showSearch)
    else
      HighLightedTextWithScroll(
        inputText  = if (isPrettyPrint) annotateJsonString(prettyGson.toJson(parsed)) else AnnotatedString(parsed.toString()),
        showSearch = showSearch)
  }
}

@Composable
private fun CollapseHeader(
  modifier   : Modifier = Modifier,
  text       : String,
  isExpanded : Boolean,
  onClick    : () -> Unit
) {
  val toggleAngle = animateFloatAsState(if (isExpanded) 90f else 0f)

  Row(
    modifier = M.fillMaxWidth().clickable { onClick() }.padding(8.dp).then(modifier),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = M
        .size(30.dp)
        .clickable { onClick() }
        .rotate(toggleAngle.value),
      imageVector        = Icons.Default.KeyboardArrowRight,
      contentDescription = null)
    Text(text, style = T.body2)
  }
}

@Composable
private fun HeadersTable(
  modifier : M = Modifier,
  headers  : Headers
) {
  Column(modifier) {
    headers.forEach { (key, value) ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          modifier = M.fillMaxWidth(0.2f),
          text     = "$key  ",
          color    = C.onSurface.copy(alpha = 0.6f),
          style    = T.body2)
        SelectionContainer(
          modifier = M.weight(1f),
          content  = { Text(text = value, style = T.body2) })
      }
    }
  }
}


private val httpCodes = mapOf(
  200 to "Ok", 401 to "Unauthorised", 404 to "Not Found", 500 to "Internal Server Error"
)

private val allHttpCodes = mapOf(
  100 to "Continue", 101 to "Switching Protocols", 102 to "Processing", 103 to "Early Hints",
  200 to "Ok", 201 to "Created", 202 to "Accepted", 203 to "Non-Authoritative Information", 204 to "No Content", 205 to "Reset Content", 206 to "Partial Content", 207 to "Multi Status", 208 to "Already Reported", 226 to "IM Used",
  300 to "Multiple Choices", 301  to "Moved Permanently", 302 to "Found", 303 to "See Other", 304 to "Not Modified", 305 to "Use Proxy", 306 to "Unused", 307 to "Temporary Redirect", 308 to "Permanent Redirect",
  400 to "Bad Request", 401 to "Unauthorised", 402 to "Payment Required", 403 to "Forbidden", 404 to "Not Found", 405 to "Method Not Allowed", 406 to "Not Acceptable", 407 to "Proxy Authentication Required", 408 to "Request Timeout", 409 to "Conflict", 410 to "Gone", 411 to "Length Required", 412 to "Precondition Failed", 413 to "Payload Too Large", 414 to "URI Too Long", 415 to "Unsupported Media Type", 416 to "Range Not Satisfiable", 417 to "Expectation Failed", 418 to "I'm a teapot", 421 to "Misdirected Request", 422 to "Unprocessable Entity ", 423 to "Locked", 424 to "Failed Dependency", 425 to "Too Early", 426 to "Upgrade Required", 428 to "Precondition Required", 429 to "Too Many Requests", 431 to "Request Header Fields Too Large", 451 to "Unavailable For Legal Reasons",
  500 to "Internal Server Error", 501 to "Not Implemented", 502 to "Bad Gateway", 503 to "Service Unavailable", 504 to "Gateway Timeout", 505 to "HTTP Version Not Supported", 506 to "Variant Also Negotiates", 507 to "Insufficient Storage", 508 to "Loop Detected", 510 to "Not Extended", 511 to "Network Authentication Required",
)

sealed class ResponseType(val value: String) {
  data class Mock(val mockBody: String, val path: String, val name: String): ResponseType(value = mockBody)
  data class Real(val realBody: String)                                    : ResponseType(value = realBody)
}

fun Gson.stringOrDefault(textValue: String) = runCatching {
  toJson(JsonParser.parseString(textValue))
}.getOrElse { textValue }.takeIf {
  it.firstOrNull() != '"' && it != "null"
} ?: textValue