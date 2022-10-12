package ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import core.*
import java.util.UUID

private val throttleCheckbox = mutableStateOf(false)

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
        if (requests.isNotEmpty()) {
          IconButton(
            onClick = { requests.removeIf { responses[it.id] != null } },
            content = { Icon(Icons.Outlined.Delete, contentDescription = null) })
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
      check     = catchEnabled.value,
      onChecked = { catchEnabled.value = catchEnabled.value.not() })
    LeftPaneRow(
      title     = "Throttle requests",
      check     = throttleCheckbox.value,
      onChecked = {
        throttleCheckbox.value = throttleCheckbox.value.not()
        if (throttleCheckbox.value.not()) throttle.value = null else throttle.value = 0
      })

    AnimatedVisibility (throttleCheckbox.value) { ThrottleSection() }
  }
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
            null -> IconButton(
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
      text  = "Delay ${throttle.value ?: 0} ms")
    Slider(
      value         = throttle.value?.toFloat() ?: 0f,
      onValueChange = { throttle.value = ((it.toLong() / 50) * 50) },
      valueRange    = 0f..10_000f,
      steps         = 200)
  }
}