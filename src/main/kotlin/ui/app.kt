package ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.onClick
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import core.mutable
import java.util.*

val pushDogDialog = mutableStateOf(false)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
  MaterialTheme(
    colors     = Palette,
    typography = Typography
  ) {
    Surface(M.fillMaxWidth().onClick { pushDogDialog.value = true }) {
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

    PushDogDialog(
      visible        = pushDogDialog.value,
      onCloseRequest = { pushDogDialog.value = !pushDogDialog.value })
  }
}