package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DraggableDivider(
  width         : Dp,
  onWidthChange : (Dp) -> Unit
) {
  val density = LocalDensity.current

  Box(modifier = M
    .fillMaxHeight()
    .width(4.dp)
    .background(C.onSurface.copy(0.2f))
    .pointerHoverIcon(PointerIcon.Hand)
    .draggable(
      state = rememberDraggableState { delta ->
        val newPaneWidth = width + with(density) { delta.toDp() }
        onWidthChange(newPaneWidth.value.coerceIn(300f..1000f).dp)
      },
      orientation = Orientation.Horizontal))
}