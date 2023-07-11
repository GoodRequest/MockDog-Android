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


val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
var showSearch       by mutableStateOf(false)
@Composable
fun Detail(id : UUID) {
  val isExpandedRequest  = mutable(false)
  val isExpandedResponse = mutable(true)

  val selectedRequest = requests.firstOrNull { it.id == id }
  val response        = responses[id]

  selectedRequest?.let {
    Column(
      modifier = M.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
        id          = it.id,
        request     = it.request,
        response    = response,
        isExpanded  = isExpandedResponse.value,
        onClick     = { isExpandedResponse.value = isExpandedResponse.value.not() })
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
  modifier   : Modifier = Modifier,
  id         : UUID,
  request    : RecordedRequest,
  response   : Response?,
  isExpanded : Boolean,
  onClick    : () -> Unit
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
          null            -> ResponseForm(id, request.path ?: "Unknown path", null)
          is Loading      -> Text(modifier = M.padding(16.dp), text = "Sending...")
          is EditResponse -> ResponseForm(id, request.path ?: "Unknown path", response.response)
          is SentResponse -> {
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
    Dialog(
      state          = rememberDialogState(size = DpSize(400.dp, 200.dp)),
      onCloseRequest = { isSaveMockDialogVisible.value = false },
      title          = "Save mock to file",
      resizable      = false,
      content = {
        val name = mutable("")

        Column (
          modifier = modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          OutlinedTextField(
            modifier      = M.fillMaxWidth(),
            maxLines      = 1,
            label         = { Text("Name of response", style = T.caption) },
            singleLine    = true,
            value         = name.value,
            onValueChange = { name.value = it })
          Spacer(modifier = M.weight(1f))
          Button(
            modifier = M.align(Alignment.End),
            enabled  = name.value.isNotBlank(),
            onClick = {
              saveFile(request.path ?: "Unknown path", name.value, response.body)
              isSaveMockDialogVisible.value = false
            }
          ) {
            Text(modifier = M.padding(horizontal = 16.dp), text = "Save")
          }
        }
      })
  }
}

@Composable
fun ResponseForm(id: UUID, path: String, editResponse: SentResponse?) {
  var codesExpanded by mutable(false)

  val parsed = try { JsonParser.parseString(editResponse?.body ?: "{}") } catch (e: Exception) {
    e.printStackTrace()
    JsonParser.parseString("{}")
  }

  val (body, setBody) = remember(id) { mutableStateOf((if (editResponse == null) "" else prettyGson.toJson(parsed))) }

  Column(modifier = M.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)) {
    val savedMocks = getSavedMockFor(path)

    // Insert own JSON
    OutlinedTextField(
      modifier      = M.fillMaxWidth().padding(bottom = 16.dp),
      label         = { Text("JSON mock", style = T.caption) },
      colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
      value         = body,
      onValueChange = { setBody(it) })

    // Response codes
    Row(modifier = M.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {

      //Real response
      Button(
        modifier = M.padding(end = 8.dp),
        onClick  = { sendRealResponse(id) },
        shape    = CircleShape
      ) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "Real")
      }

      //All responses
      Button(
        modifier = M.padding(end = 8.dp),
        onClick  = { codesExpanded = !codesExpanded },
        shape    = CircleShape
      ) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "All")
      }

      // HTTP codes
      httpCodes.forEach { entry ->
        Button(onClick  = { sendMockResponse(id = id, url = editResponse?.url, code = entry.key, body = body) }, shape = CircleShape) {
          Text(modifier = M.padding(horizontal = 8.dp), text = "${entry.key}")
        }
      }

      DropdownMenu(
        modifier         = M.fillMaxHeight(0.4f),
        expanded         = codesExpanded,
        onDismissRequest = { codesExpanded = false }
      ) {
        allHttpCodes.forEach { entry ->
          DropdownMenuItem(
            onClick = {
              sendMockResponse(id = id, url = editResponse?.url, code = entry.key, body = body)
              codesExpanded = false
            }
          ) {
            Text(text = "${entry.key} - ${entry.value}")
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
          Button(onClick  = { setBody(readFile(file) ?: "Cannot read file") }, shape = CircleShape) {
            Text(modifier = M.padding(horizontal = 8.dp), text = file.substringAfterLast("_"))
          }
        }
      }
    }
  }
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