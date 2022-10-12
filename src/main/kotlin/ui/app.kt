package ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import core.mutable
import java.util.UUID

@Composable
fun App() {
  MaterialTheme(
    colors     = Palette,
    typography = Typography
  ) {
    Surface(M.fillMaxWidth()) {
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
}