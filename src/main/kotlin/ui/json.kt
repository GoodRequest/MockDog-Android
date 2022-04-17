package ui.json

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import theme.BlueLight
import theme.IconBlue
import theme.PrimeBlack
import theme.Yellow

//val example = """
//    {"menu": {
//      "id": "file",
//      "value": "File",
//      "popup": {
//        "menuitem": [
//          {"value": "New", "onclick": "CreateNewDoc()"},
//          {"value": "Open", "onclick": "OpenDoc()"},
//          {"value": "Close", "onclick": "CloseDoc()"}
//        ]
//      }
//    }}
//"""
//
//
//@Composable
//fun JsonView() {
//    val parsed = JsonParser.parseString(example)
//    val collapsed = remember { mutableStateListOf<String>() }
//
//    JsonTree(parsed, null, "", collapsed)
//}

val keyColor = IconBlue//AppTheme.code.jsonKey.color
val bracesColor = PrimeBlack//AppTheme.code.jsonPunctuation.color
val textSize = 12.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JsonTree(root: JsonElement, key: String?, path: String, collapsed: SnapshotStateList<String>, onKeySelected: (String) -> Unit) {
  val offset = 15.dp
  val fullPath = if(key != null) "$path/$key" else path

  when {
    root.isJsonArray -> {
      Column(modifier = Modifier.padding(horizontal = offset, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if(key != null)
            Text("$key: ", color = keyColor, fontSize = textSize)

          Text("[", color = bracesColor, fontSize = textSize)

          if(root.asJsonArray.isEmpty.not())
            Icon(
              modifier = Modifier
                .size(24.dp)
                .rotate(if(fullPath in collapsed) 270f else 0f)
                .clickable {
                  if(fullPath in collapsed) collapsed.remove(fullPath)
                  else                      collapsed.add(fullPath)
                },
              imageVector         = Icons.Default.ArrowDropDown,
              contentDescription  = "Search",
              tint                = keyColor)

          if(fullPath in collapsed) {
            Text(if(root.asJsonArray.isEmpty) "]" else "${root} ]",
              color = bracesColor,
              fontSize = textSize,
              overflow = TextOverflow.Ellipsis,
              maxLines = 1)
          }
        }
        if(fullPath !in collapsed) {
          root.asJsonArray.forEachIndexed { index, item ->
            JsonTree(item, null, "$fullPath[$index]", collapsed, onKeySelected)
          }
          Text("]", color = bracesColor, fontSize = textSize)
        }
      }
    }
    root.isJsonObject -> {
      Column(modifier = Modifier.padding(horizontal = offset, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if(key != null) Text(
            modifier = Modifier.clickable { onKeySelected("$fullPath") },
            text     = "$key: ",
            fontSize = textSize,
            color    = keyColor)

          Text("{", color = bracesColor, fontSize = textSize)

          if(root.asJsonObject.size() != 0)
            Icon(
              modifier = Modifier
                .size(24.dp)
                .rotate(if(fullPath in collapsed) 270f else 0f)
                .combinedClickable(
                  onClick     = {
                    if(fullPath in collapsed) collapsed.remove(fullPath)
                    else                      collapsed.add(fullPath)
                  },
                  onLongClick = {
                    val children = root.asJsonObject.entrySet().map { (key, _) -> "$fullPath/$key" }
                    collapsed.addAll(children)
                  }),
              imageVector         = Icons.Default.ArrowDropDown,
              contentDescription  = "Search",
              tint                = keyColor)

          if(fullPath in collapsed) {
            Text(
              text = if(root.asJsonObject.size() == 0) "}" else "$root }",
              fontSize = textSize,
              color = bracesColor,
              overflow = TextOverflow.Ellipsis,
              maxLines = 1)
          }
        }
        if(fullPath !in collapsed) {
          root.asJsonObject.entrySet().forEach { (key, value) ->
            JsonTree(value, key, fullPath, collapsed, onKeySelected)
          }
          Text("}", color = bracesColor, fontSize = textSize)
        }
      }
    }
    else -> {
      Row(modifier = Modifier.padding(horizontal = offset)) {
        Text(
          text     = "$key: ",
          fontSize = textSize,
          color    = keyColor)
        Text(
          text         = "$root",
          // onValueChange = {},
          fontSize = textSize,
          color = PrimeBlack
        )
      }
    }
  }
}

