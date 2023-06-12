package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import core.*
import java.net.InetAddress
import java.util.*

val showWhiteList = mutableStateOf(false)

@Composable
fun LeftPane(
  paneWidth : Dp,
  selected  : UUID?,
  onSelect  : (UUID) -> Unit
) {
  Column(M.fillMaxHeight().width(paneWidth).background(C.surface)) {
    Column(M.weight(1f)) {
      Row {
        Text(
          modifier = M.padding(16.dp).weight(1f),
          text = "Request history",
          style = T.subtitle1)
        if (requests.any { responses[it.id] is EditResponse  || responses[it.id] == null }) {
          IconButton(
            onClick = { sendRealResponseAll() },
            content = { Icon(Icons.Default.Send, contentDescription = null) })
        }
        if (requests.isNotEmpty()) {
          IconButton(
            onClick = { requests.removeIf { responses[it.id] != null && responses[it.id] !is EditResponse } },
            content = { Icon(Icons.Default.Delete, contentDescription = null) })
        }
      }
      RequestHistory(
        requests = requests,
        selected = selected,
        onSelect = onSelect)
    }
    Divider(M.height(1.dp))
    LeftPaneRow(
      title     = "Mock responses",
      check     = mockingEnabled.value,
      onChecked = { mockingEnabled.value = mockingEnabled.value.not() })
    AnimatedVisibility (mockingEnabled.value) {
      Box(M.padding(start = 8.dp)) {
        LeftPaneRow(
          title     = "Mock real responses",
          check     = mockRealResponse.value,
          onChecked = { mockRealResponse.value = mockRealResponse.value.not() })
      }
    }
    LeftPaneRow(
      title     = "Throttle requests",
      check     = throttle.value.isEnabled,
      onChecked = { throttle.value = throttle.value.copy(isEnabled = throttle.value.isEnabled.not()) })
    AnimatedVisibility (throttle.value.isEnabled) { ThrottleSection() }
    LeftPaneRow(
      title     = "Use real device",
      check     = useDevice.value,
      onChecked = {
        useDevice.value = useDevice.value.not()
        restartServer(useDevice.value)
      })
    AnimatedVisibility (useDevice.value) {
      Text("IP: ${InetAddress.getLocalHost().hostAddress}", M.padding(start = 16.dp, bottom = 8.dp))
    }
  }

  if (showWhiteList.value)
    Dialog(
      state          = rememberDialogState(size = DpSize(1000.dp, 800.dp)),
      onCloseRequest = { showWhiteList.value = false },
      title          = "White list for mock responses",
      resizable      = true,
      content = {
        Column (modifier = Modifier.padding(16.dp)) {
          Text(
            modifier   = M.padding(bottom = 16.dp),
            text       = "NOTE: Every request in white list is not stopped by mock responses and is sent directly!",
            fontWeight = FontWeight.Bold)
          Button(
            onClick  = {
              whiteListRequests.clear()
              saveWhiteList(requestLine = "", isAdd = false)
              showWhiteList.value = false
            }
          ) {
            Text(modifier = M.padding(horizontal = 16.dp), text = "Delete whole white list")
          }

          Column(modifier = Modifier.padding(top = 8.dp).verticalScroll(rememberScrollState())) {
            whiteListRequests.forEachIndexed { index, requestLine ->
              Row {
                Button(
                  modifier = Modifier.padding(end = 16.dp),
                  onClick  = { saveWhiteList(requestLine = requestLine, isAdd = false) }
                ) { Text(text = "Delete") }

                Text(text = "${index + 1} ► $requestLine", modifier = Modifier.align(Alignment.CenterVertically))
              }
            }
          }
        }
      })
}

@Composable
private fun RequestHistory(
  requests : SnapshotStateList<Request>,
  selected : UUID?,
  onSelect : (UUID) -> Unit
) {
  val lazyListState = rememberLazyListState()

  LaunchedEffect(requests.lastOrNull()) {
    if(requests.isNotEmpty()) lazyListState.animateScrollToItem(requests.indices.last)
  }

  if(requests.isEmpty()) Text("There are no requests yet", M.padding(horizontal = 16.dp))

  LazyColumn(
    modifier      = M.background(C.surface),
    state         = lazyListState,
    reverseLayout = true,
  ) {
    itemsIndexed(
      items = requests,
      key   = { _, item -> item.id }
    ) { index, request ->
      val isSelected = (request.id == selected)

      val isRequestInWhiteList = whiteListRequests.contains(request.request.requestLine)

      ContextMenuArea(items = { listOf(
        ContextMenuItem(if (isRequestInWhiteList) "Delete from White List" else "add to White List") {
          saveWhiteList(requestLine = request.request.requestLine, isAdd = isRequestInWhiteList.not()) }) }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = M
            .fillParentMaxWidth()
            .background(when {
              isSelected     -> BlueLight
              index % 2 == 1 -> C.surface
              else           -> C.background
            })
            .clickable { onSelect(request.id) }
            .padding(end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
          Row(
            modifier = M.width(52.dp),
            horizontalArrangement = Arrangement.Center
          ) {
            when(val response = responses[request.id]) {
              null, is EditResponse -> IconButton(
                onClick = { sendRealResponse(request.id) },
                content = { Icon(Icons.Rounded.Send, contentDescription = null) })
              is Loading      -> Text(text  = "...", style = T.body2)
              is SentResponse -> Text(text  = response.status.toString(), style = T.body2)
            }
          }
          Text(
            modifier   = M.padding(end = 16.dp),
            text       = request.request.method ?: "Unknown method",
            fontWeight = FontWeight.Medium,
            color = when(request.request.method) {
              "GET"    -> Green
              "DELETE" -> Red
              else     -> Orange
            },
            style = T.body2)
          Text(
            modifier = M.weight(1f),
            text     = request.request.path ?: "Unknown path",
            style    = T.body2)
        }
      }
    }
  }
}

@Composable
private fun LeftPaneRow(
  title     : String,
  check     : Boolean,
  onChecked : (Boolean) -> Unit
) {
  Row(
    modifier          = M.fillMaxWidth().clickable { onChecked(check) },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Checkbox(checked = check, onCheckedChange = onChecked)
    Text(style = T.body2, text = title)
  }
}

@Composable
private fun ThrottleSection() {
  Column(
    modifier            = M.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      style = T.body2,
      text  = "Delay ${throttle.value.delay} ms after every 10 bytes")
    Slider(
      value         = throttle.value.delay.toFloat(),
      onValueChange = { throttle.value = throttle.value.copy(delay = it.toLong()) },
      valueRange    = 0f..1_000f,
      steps         = 200)
  }
}