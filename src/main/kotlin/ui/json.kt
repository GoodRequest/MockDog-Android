package ui.json

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonElement
import theme.IconBlue
import theme.PrimeBlack

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

private val keyColor    = IconBlue
private val bracesColor = PrimeBlack
private val textSize    = 14.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JsonTree(root: JsonElement, key: String?, path: String, collapsed: SnapshotStateList<String>, onKeySelected: (String) -> Unit) {
  val offset = 16.dp
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
            SelectionContainer {
              Text(
                if (root.asJsonArray.isEmpty) "]" else "$root ]",
                color = bracesColor,
                fontSize = textSize,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1)
            }
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
            modifier = Modifier.clickable { onKeySelected(fullPath) },
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
            SelectionContainer {
              Text(
                text     = if(root.asJsonObject.size() == 0) "}" else "$root }",
                fontSize = textSize,
                color    = bracesColor,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1)
            }
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
      SelectionContainer {
        Row(modifier = Modifier.padding(horizontal = offset)) {
          Text(
            text     = "$key: ",
            fontSize = textSize,
            color    = keyColor)
          Text(
            text     = "$root",
            // onValueChange = {},
            fontSize = textSize,
            color    = PrimeBlack)
        }
      }
    }
  }
}