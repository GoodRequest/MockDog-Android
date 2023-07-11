package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import core.mockFiles
import core.mutable
import core.readFile
import java.util.*

data class GlobalResponse(
  val code: Int,
  val name: String,
  val body: String
)

var globalResponse       by mutableStateOf<GlobalResponse?>(null)
var globalResponseDialog by mutableStateOf(false)

@Composable
fun App() {
  MaterialTheme(
    colors     = Palette,
    typography = Typography
  ) {
    Surface(M.fillMaxWidth()) {
      Column {
        // Global response status
        globalResponse?.let { response ->
          ContextMenuArea(items = { listOf(ContextMenuItem("Remove global response") { globalResponse = null }) }) {
            Row(
              modifier              = M.fillMaxWidth().background(color = Orange),
              horizontalArrangement = Arrangement.Center
            ) {
              Text(
                text       = "☺ Warning ☻ global response is set to ► ${response.code} - ${response.name}",
                fontWeight = FontWeight.Bold)
            }
          }
        }

        // App content
        Row {
          val leftPaneWidth   = mutable(400.dp)
          val selectedRequest = mutable<UUID?>(null)

          // Left pane - history + settings
          LeftPane(
            paneWidth = leftPaneWidth.value,
            selected  = selectedRequest.value,
            onSelect  = { selectedRequest.value = it })

          // Movable divider
          DraggableDivider(
            width         = leftPaneWidth.value,
            onWidthChange = { leftPaneWidth.value = it })

          // Detail
          Surface(
            modifier = M.fillMaxHeight().weight(1f),
            color    = C.background
          ) {
            selectedRequest.value?.let { Detail(id = it) }
          }
        }
      }
    }

    if (globalResponseDialog)
      GlobalResponseDialog()
  }
}

@Composable
private fun GlobalResponseDialog() {
  val (body, setBody)             = mutable("")
  val (mockDialog, setMockDialog) = mutable(false)
  val lazyState                   = rememberLazyListState()

  Dialog(
    state          = rememberDialogState(size = DpSize(600.dp, 900.dp)),
    onCloseRequest = { globalResponseDialog = false },
    title          = "Global request response",
    resizable      = true,
    content        = {
      Column (
        modifier            = M.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          modifier = M.align(Alignment.CenterHorizontally),
          text     = "Set global response for every request.",
          style    = T.h6)

        // Insert own JSON
        OutlinedTextField(
          modifier      = M.fillMaxWidth().padding(bottom = 16.dp).weight(1f),
          label         = { Text("JSON response body", style = T.caption) },
          colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
          value         = body,
          onValueChange = setBody)

        Button(onClick = { setMockDialog(true) }) {
          Text(text = "Load saved mock")
        }

        Text(text = "Response codes:")

        Box(modifier = M.fillMaxWidth().weight(1f)) {
          LazyColumn(state = lazyState) {
            allHttpCodes.forEach { entry ->
              item {
                Text(
                  text = "${entry.key} - ${entry.value}",
                  style = T.subtitle1,
                  modifier = M.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    globalResponse = GlobalResponse(
                      code = entry.key,
                      name = entry.value,
                      body = body
                    )

                    globalResponseDialog = false
                  })
              }
            }
          }

          VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter  = rememberScrollbarAdapter(lazyState)
          )
        }
      }
    })

  if (mockDialog)
    Dialog(
      state          = rememberDialogState(size = DpSize(400.dp, 600.dp)),
      onCloseRequest = { setMockDialog(false) },
      title          = "Load saved mock",
      resizable      = true,
      content        = {
        Column (
          modifier            = M.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Text(
            modifier = M.align(Alignment.CenterHorizontally),
            text     = "Load saved mock from file",
            style    = T.subtitle1)

          Column(M.verticalScroll(rememberScrollState()).weight(1f)) {
            mockFiles.value.filter {
              it.contains('_')
            }.forEach {
              val requestPath = it.substringAfterLast('_')
              Text(
                modifier = M.fillMaxWidth().padding(vertical = 4.dp).clickable {
                  readFile(it)?.let(setBody)
                  setMockDialog(false)
                },
                text = requestPath
              )
            }
          }
        }
      })
}