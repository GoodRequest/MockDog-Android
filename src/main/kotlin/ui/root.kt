package mockdog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import core.*
import okhttp3.Headers
import okhttp3.mockwebserver.MockResponse
import theme.*
import ui.LeftPane
import ui.json.JsonTree
import java.awt.Cursor
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.*
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.filechooser.FileSystemView

private val listWidth         = mutableStateOf(400.dp)
private val selectedRequest   = mutableStateOf<UUID?>(null)
private val expandedRequest   = mutableStateMapOf<UUID, Unit>()
private val collapsedResponse = mutableStateMapOf<UUID, Unit>()
private val mockDir           = mutableStateOf("")

private fun SnapshotStateMap<UUID, Unit>.toggle(key: UUID) = if (containsKey(key)) remove(key) else put(key, Unit)

@Composable
fun App() {
  mockDir.value = File("mocks/saveDir.txt").readText()
  MaterialTheme(
    colors     = ColorPalette,
    typography = TypographyTypes
  ) {
    Surface(M.fillMaxWidth()) {
      Row {
        // left pane - history + settings
        LeftPane(
          listWidth = listWidth.value,
          selected  = selectedRequest.value,
          onSelect  = { selectedRequest.value = it })
        // movable divider
        val dpi = LocalDensity.current
        Box(M.width(4.dp)
          .fillMaxHeight()
          .background(Color.Black.copy(alpha = 0.2f))
          .draggable(rememberDraggableState { delta -> listWidth.value += with(dpi) { delta.toDp() } }, Orientation.Horizontal))

        // detail
        selectedRequest.value?.let { id ->
          val (_, request, body)  = requests.firstOrNull { it.id == id }!!
          val response            = responses[id]
          val isCollapsedRequest  = expandedRequest[id]  == null
          val isCollapsedResponse = collapsedResponse[id] != null

          Column(M.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(
              modifier = M.padding(bottom = 16.dp),
              text  = request.method + " " + request.path!!,
              style = T.subtitle1)

            // request
            Column(M.background(color = Color.White, shape = SH.medium).fillMaxWidth()) {
              // request header
              Row(M.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  modifier = M
                    .size(30.dp)
                    .rotate(isCollapsedRequest(270f, 0f))
                    .clickable { expandedRequest.toggle(id) },
                  imageVector        = Icons.Default.ArrowDropDown,
                  contentDescription = "")
                Text("Request", style = T.caption)
              }
              // request body
              AnimatedVisibility(isCollapsedRequest.not()) {
                Column {
                  HeadersTable(request.headers)
                  if (body.isNotBlank()) {
                    val (isFormatted, setFormatted) = remember { mutableStateOf(false) }

                    if(request.headers.values("content-type").any { it.contains("application/json", ignoreCase = true) })
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(isFormatted, setFormatted)
                        Text("Format JSON", fontSize = 12.sp, color = C.onSurface.copy(alpha = 0.6f))
                      }

                    if(isFormatted) {
                      val parsed = try { JsonParser.parseString(body) } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        JsonParser.parseString("{}")
                      } // TODO performance
                      val collapsed = remember { mutableStateListOf<String>() }
                      JsonTree(parsed, null, "", collapsed, {})
                    } else {
                      SelectionContainer {
                        Text(
                          modifier = M.padding(16.dp),
                          fontSize = 14.sp,
                          text     = body)
                      }
                    }
                  }
                }
              }
            }

            Spacer(M.height(16.dp))

            // response
            Column(M.background(color = Color.White, shape = SH.medium).fillMaxWidth()) {
              // response header
              Row(M.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(modifier = M
                    .size(30.dp)
                    .rotate(isCollapsedResponse(270f, 0f))
                    .clickable { collapsedResponse.toggle(id) },
                  imageVector         = Icons.Default.ArrowDropDown,
                  contentDescription  = "")
                Text("Response", style = T.caption)
              }
              // response body
              AnimatedVisibility(isCollapsedResponse.not()) {
                when(response) {
                  null -> ResponseForm(id, request.path!!)
                  is Loading -> Text(modifier = M.padding(16.dp), text = "Sending...")
                  is SentResponse -> {
                    Column {
                      // Status code + url
                      Text(
                        modifier = M.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        text     = MockResponse().setResponseCode(response.status).status + " (${response.duration}ms)")
                      Text(
                        modifier = M.padding(horizontal = 16.dp, vertical = 4.dp),
                        text     = response.url,
                        style    = T.body2,
                        color    = C.onSurface.copy(alpha = 0.6f))
                      // Headers
                      HeadersTable(response.headers)

                      val (isFormatted, setFormatted) = remember { mutableStateOf(false) }

                      if(response.headers.values("content-type").any { it.contains("application/json", ignoreCase = true) })
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Checkbox(isFormatted, setFormatted)
                          Text("Format JSON", fontSize = 12.sp, color = C.onSurface.copy(alpha = 0.6f))
                        }

                      if(isFormatted) {
                        val parsed = try { JsonParser.parseString(response.body) } catch (e: java.lang.Exception) {
                          e.printStackTrace()
                          JsonParser.parseString("{}")
                        } // TODO performance
                        val collapsed = remember { mutableStateListOf<String>() }
                        JsonTree(parsed, null, "", collapsed, {})
                      } else {
                        SelectionContainer {
                          Text(
                            modifier = M.padding(16.dp),
                            fontSize = 14.sp,
                            text     = response.body)
                        }
                      }

                      if (response.duration != 0L) // is not mock
                        SaveMockItemsRow(
                          modifier   = M.padding(horizontal = 16.dp),
                          body       = response.body,
                          setBody    = null,
                          setBodyErr = null,
                          path       = request.path!!)
                      }
                  }
                }
              }
            }
          }
        }
        Dialog()
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
        SelectionContainer {
          Text(
            text = value,
            modifier = M.horizontalScroll(rememberScrollState()),
            fontSize = 12.sp,
            maxLines = 1)
        }
      }
    }
  }
}

@Composable
fun ResponseForm(id: UUID, path: String) {
  val codes                 = remember { listOf(200, 401, 404, 500) }
  val (body, setBody)       = mutable("")
  val (bodyErr, setBodyErr) = mutable(false)

  Column(M.fillMaxWidth().padding(horizontal = 16.dp)) {

    Column {
      // pomocny val kde mam ulozeny string requestu ale len cast pred query,
      // tento val nasledne pouzivame na porovnanie(vo filtri) s nazvami suborov ulozenych v namesList
      val requestPath = path.substringBefore("?").replace("/", "_").replace("?", "_")
      // zobrazenie prislunych file-ov ku danemu requestu v podobe tlacidiel s nazvom daneho file-u
      val savedMocks = savedMocksFor(requestPath)
      if(savedMocks.isNotEmpty()) {
        Row(M.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(text = "Saved:", style = T.caption)
          savedMocks.forEach { file ->
            ClickableText(
              style = T.button,
              text = buildAnnotatedString { withStyle(SpanStyle(
                fontSize = 16.sp,
                color = VioletDark,
                textDecoration = TextDecoration.Underline)) {
                append(file.substringAfterLast("_"))
              } },
              onClick = { setBody(readFile(name = file)) })
          }
        }
      }

      OutlinedTextField(
        modifier      = M.padding(bottom = 16.dp).fillMaxWidth(),
        maxLines      = 15,
        label         = { Text("JSON mock", M.padding(top = 4.dp), style = T.caption) },
        colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
        isError       = bodyErr,
        value         = body,
        onValueChange = { setBody(it); setBodyErr(false) })

      if(body.isNotBlank())
        ClickableText(
          text = buildAnnotatedString { withStyle(SpanStyle(color = Color.Black)) { append("format json") } },
          onClick = {
            val gson = GsonBuilder().setPrettyPrinting().create()
            val element = JsonParser.parseString(body)
            setBody(gson.toJson(element))
          })

      if (bodyErr) { ErrorText("Fill JSON response") }
    }

    DogTitleText(text = "Send response with code")

    Row(M.padding(bottom = 16.dp, top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      codes.forEach { code ->
        Button(onClick  = { sendMockResponse(id, code, body) }, shape = CircleShape) {
          Text(modifier = M.padding(horizontal = 8.dp), text = "$code")
        }
      }
      Button(onClick  = { sendRealResponse(id) }, shape = CircleShape) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "REAL")
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
  Column() {


    Row(
      modifier = modifier.padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Column {
        TextField(
          modifier = M.height(54.dp).width(200.dp),
          maxLines = 1,
          label = { Text("name of response", M.padding(top = 4.dp), style = T.caption) },
          colors = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = Color.White),
          isError = nameErr,
          value = name,
          onValueChange = { setName(it); setNameErr(false) })

        if (nameErr) {
          ErrorText("enter mock name:")
        }
      }

      Button(
        onClick = {
          when {
            name.isEmpty() || body.isEmpty() -> {
              if (name.isEmpty()) setNameErr(true)
              if (body.isEmpty() && setBodyErr != null) setBodyErr(true)
            }
            else -> {
              saveFile(path, name, body)
              setName("")
              setBody?.invoke("")
            }
          }
        },
        modifier = M.padding(top = 10.dp)
      ) {
        Text(modifier = M.padding(horizontal = 16.dp), text = "SAVE")
      }
    }
    ClickableText(
      modifier = M.padding(horizontal = 16.dp).padding(bottom = 32.dp).pointerHoverIcon(icon = PointerIcon(Cursor.getPredefinedCursor(12))),
      maxLines = 1,
      text = buildAnnotatedString {
        append("Response save directory: ")
        withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
          append(mockDir.value)
        }
      } ,
      onClick = {

        // https://github.com/JetBrains/compose-jb/issues/1003
        val fileChooser = JFileChooser(FileSystemView.getFileSystemView())
        fileChooser.currentDirectory = File(System.getProperty("user.dir"))
        fileChooser.dialogTitle = "Choose directory"
        fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        fileChooser.isAcceptAllFileFilterUsed = true
        fileChooser.selectedFile = null
        fileChooser.currentDirectory = File(mockDir.value)
        if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
          val file = fileChooser.selectedFile

          mockDir.value = file.absolutePath
          File("mocks/saveDir.txt").writeText(file.absolutePath)
          loadMocks()
        }
      }
    )
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Dialog() {
  if (errorDialog.value.first) {
    AlertDialog(
      onDismissRequest = {
        // Dismiss the dialog when the user clicks outside the dialog or on the back
        // button. If you want to disable that functionality, simply use an empty
        // onCloseRequest.
        errorDialog.value = Pair(false, "")
      },
      title = {
        Row {
          Icon(imageVector = Icons.Default.Warning, contentDescription = null,modifier = M.padding(end = 8.dp))
          Text(text = "Error")
        }

      },
      text = {
        Text(errorDialog.value.second)
      },
      modifier = M.defaultMinSize(300.dp),
      confirmButton = {
        Button(
          onClick = {
            errorDialog.value = Pair(false, "")
          }) {
          Text("Ok!")
        }
      })
  }

}

@Composable
fun openFilePicker(){
  val text = remember { mutableStateOf("Hello, World!") }
  val isFileChooserOpen = remember { mutableStateOf(false) }
  if (isFileChooserOpen.value) {
    FileDialog(

      onCloseRequest = {
        isFileChooserOpen.value = false
        println("Result $it")
      }
    )
  }
  MaterialTheme {
    Button(onClick = {
      isFileChooserOpen.value = true
    }) {
      Text(text.value)
    }
  }



}

@Composable
private fun FileDialog(
  parent: Frame? = null,
  onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
  create = {
    object : FileDialog(parent, "Choose a file", LOAD) {
      override fun setVisible(value: Boolean) {
        super.setVisible(value)
        if (value) {
          onCloseRequest(file)
        }
      }
    }
  },
  dispose = FileDialog::dispose
)

@Composable fun <A> mutable(init: A) = remember { mutableStateOf(init) }
operator fun <A> Boolean.invoke(ifTrue: A, ifFalse: A) = if(this) ifTrue else ifFalse