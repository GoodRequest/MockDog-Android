package ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.rememberDialogState
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import core.mutable
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import windowState
import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.math.max

private val jsonKey        : SpanStyle = SpanStyle(Color(0xFF4091cf))
private val jsonPunctuation: SpanStyle = SpanStyle(Color(0xFF808080))
fun annotateJsonString(str: String) = buildAnnotatedString {
  append(str)
  addStyle(str, jsonKey, Regex("""("[^"]+?)"\s*:"""))
  addStyle(str, jsonPunctuation, ":")
  addStyle(str, jsonPunctuation, "=")
  addStyle(str, jsonPunctuation, "[")
  addStyle(str, jsonPunctuation, "]")
  addStyle(str, jsonPunctuation, "{")
  addStyle(str, jsonPunctuation, "}")
  addStyle(str, jsonPunctuation, "(")
  addStyle(str, jsonPunctuation, ")")
  addStyle(str, jsonPunctuation, ",")
}

private fun AnnotatedString.Builder.addStyle(str:String, style: SpanStyle, regexp: String) {
  addStyle(str, style, Regex.fromLiteral(regexp))
}

private fun AnnotatedString.Builder.addStyle(str: String, style: SpanStyle, regexp: Regex) {
  for (result in regexp.findAll(str)) {
    addStyle(style, result.range.first, result.range.last + 1)
  }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun HighLightedTextWithScroll(
  inputText  : AnnotatedString,
  showSearch : Boolean
) {
  val (search, setSearch) = remember(showSearch, inputText) { mutableStateOf("") }
  val bringer             = remember { BringIntoViewRequester() }

  val textToHighlight by MutableStateFlow(search).debounce(150).distinctUntilChanged().collectAsState("")

  val highlightStartIndexes by remember(textToHighlight, inputText) {
    derivedStateOf {
      getIndexesToHighlight(inputText = inputText, textToHighlight = textToHighlight)
    }
  }
  val text = rememberHighlightText(inputText = inputText, textToHighlight = textToHighlight, highlightIndexes = highlightStartIndexes)
  var textFieldValue by remember(text) { mutableStateOf(TextFieldValue(text)) }

  val (selected, setSelected) = remember(highlightStartIndexes) { mutableStateOf(0) }

  val columnHeight by remember {
    derivedStateOf {
      (windowState.size.height.value * 0.85).dp
    }
  }

  Column(modifier = M.fillMaxWidth().padding(horizontal = 16.dp).then(if (showSearch) M.heightIn(max = columnHeight) else M)) {
    if (showSearch) {
      searchView(
        search         = search,
        setSearch      = setSearch,
        resultListSize = highlightStartIndexes.size,
        selected       = selected,
        setSelected    = setSelected
      )

      LaunchedEffect(selected, textToHighlight) {
        if (highlightStartIndexes.isNotEmpty())
          textFieldValue = textFieldValue.copy(selection = TextRange(highlightStartIndexes.elementAt(selected), highlightStartIndexes.elementAt(selected) + textToHighlight.length))
      }
    }

    BasicTextField(
      modifier      = M.bringIntoViewRequester(bringer),
      value         = textFieldValue,
      onValueChange = { textFieldValue = it.copy(annotatedString = text) },
      textStyle     = LocalTextStyle.current.copy(
        color    = PrimeBlack,
        fontSize = 14.sp))
  }

  LaunchedEffect(textToHighlight, selected) {
    if (textToHighlight.isNotBlank()) {
      delay(10)
      bringer.bringIntoView()
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun EditableTextWithSearch(
  value        : String,
  onValueChange: (String) -> Unit,
  modifier     : Modifier = Modifier,
  label        : @Composable (() -> Unit)?,
  colors       : TextFieldColors = TextFieldDefaults.outlinedTextFieldColors()
) {
  if (showSearch) {
    val (search, setSearch) = remember(showSearch) { mutableStateOf("") }
    val bringer = remember { BringIntoViewRequester() }

    val textToHighlight by MutableStateFlow(search).debounce(150).distinctUntilChanged().collectAsState("")

    val highlightStartIndexes by remember(textToHighlight, value) {
      derivedStateOf {
        getIndexesToHighlight(inputText = AnnotatedString(value), textToHighlight = textToHighlight)
      }
    }

    var textFieldValue by remember { mutableStateOf(TextFieldValue(AnnotatedString(value))) }

    LaunchedEffect(value, textToHighlight) {
      val highlightText = buildAnnotatedString {
        append(value)
        highlightStartIndexes.map {
          addStyle(
            style = SpanStyle(background = Color.Yellow),
            start = it,
            end = it + textToHighlight.length
          )
        }
      }

      textFieldValue = textFieldValue.copy(annotatedString = highlightText)
    }

    val (selected, setSelected) = remember(highlightStartIndexes) { mutableStateOf(0) }

    val columnHeight by remember {
      derivedStateOf {
        (windowState.size.height.value * 0.70).dp
      }
    }

    Column(modifier = M.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = columnHeight)) {
      if (showSearch) {
        searchView(
          search = search,
          setSearch = setSearch,
          resultListSize = highlightStartIndexes.size,
          selected = selected,
          setSelected = setSelected
        )

        LaunchedEffect(selected, textToHighlight) {
          if (highlightStartIndexes.isNotEmpty())
            textFieldValue = textFieldValue.copy(
              selection = TextRange(
                highlightStartIndexes.elementAt(selected),
                highlightStartIndexes.elementAt(selected) + textToHighlight.length
              )
            )
        }
      }

      OutlinedTextField(
        modifier = modifier.bringIntoViewRequester(bringer),
        label = label,
        colors = colors,
        value = textFieldValue,
        onValueChange = {
          val highlightText = buildAnnotatedString {
            append(it.text)
            highlightStartIndexes.map {
              addStyle(
                style = SpanStyle(background = Color.Yellow),
                start = it,
                end = it + textToHighlight.length
              )
            }
          }

          onValueChange(it.text)
          textFieldValue = it.copy(annotatedString = highlightText)
        })
    }

    LaunchedEffect(textToHighlight, selected) {
      if (textToHighlight.isNotBlank()) {
        delay(10)
        bringer.bringIntoView()
      }
    }
  } else {
    OutlinedTextField(
      modifier      = modifier,
      label         = label,
      colors        = colors,
      value         = value,
      onValueChange = onValueChange)
  }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
fun MocDogJsonParser(
  element    : JsonElement,
  showSearch : Boolean
) {
  val collapsed  = remember { mutableStateListOf<String>() }
  val dialogSize by remember {
    derivedStateOf {
      DpSize(width = (windowState.size.width.value * 0.9).dp, height = (windowState.size.height.value * 0.9).dp)
    }
  }
  val dialogState                           = rememberDialogState(size = dialogSize)
  val (jsonArrayDialog, setJsonArrayDialog) = mutable<Pair<String, JsonArray>?>(null)    // first = key of json array, second = actual json array
  val (search, setSearch)                   = remember(showSearch, element, jsonArrayDialog) { mutableStateOf("") }

  val textToHighlight by MutableStateFlow(search).debounce(150).distinctUntilChanged().collectAsState("")
  var highlightCount  by remember(textToHighlight, collapsed.size) { mutableStateOf(0) }
  var bringer         by remember(textToHighlight, collapsed.size) { mutableStateOf<List<BringIntoViewRequester>>(listOf()) }

  val (selected, setSelected) = remember(bringer) { mutableStateOf(0) }

  @Composable
  fun HighLightedText(
    modifier  : Modifier = Modifier,
    inputText : AnnotatedString,
    overflow  : TextOverflow = TextOverflow.Clip,
    maxLines  : Int          = Int.MAX_VALUE
  ) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val highlightStartIndexes  by remember {
      derivedStateOf {
        getIndexesToHighlight(inputText = inputText, textToHighlight = textToHighlight)
      }
    }

    val text = rememberHighlightText(inputText = inputText, textToHighlight = textToHighlight, highlightIndexes = highlightStartIndexes)

    DisposableEffect(text, highlightStartIndexes, collapsed.size) {
      val bringerList = highlightStartIndexes.map { bringIntoViewRequester }

      highlightCount += highlightStartIndexes.size
      if (highlightStartIndexes.isNotEmpty()) bringer += bringerList

      onDispose {
        highlightCount -= highlightStartIndexes.size
        bringer         = bringer.filter { it !in bringerList }
      }
    }

    Text(
      modifier = modifier.then(if (highlightStartIndexes.isNotEmpty()) M.bringIntoViewRequester(bringIntoViewRequester) else M),
      text     = text,
      color    = PrimeBlack,
      fontSize = 14.sp,
      overflow = overflow,
      maxLines = maxLines
    )
  }

  @Composable
  fun JsonText(
    modifier : Modifier = Modifier,
    key      : String?,
    root     : String
  ) {
    val realKey        = if(key != null) "$key: " else ""
    val completeString = "$realKey$root"

    HighLightedText(
      modifier  = modifier,
      inputText = buildAnnotatedString {
        append(completeString)
        key?.let {
          addStyle(
            style = SpanStyle(color = Blue),
            start = completeString.indexOf(realKey),
            end   = completeString.lastIndexOf(realKey) + realKey.length)
        }
      })
  }

  @Composable
  fun JsonTree(root: JsonElement, key: String?, path: String, collapsed: SnapshotStateList<String>) {
    val keyColor = Blue
    val offset   = 16.dp
    val fullPath = if(key != null) "$path/$key" else path

    when {
      root.isJsonArray -> {
        Column(modifier = Modifier.padding(horizontal = offset, vertical = 4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            JsonText(
              key  = key,
              root = "["
            )

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
                tint                = keyColor
              )

            if(fullPath in collapsed) {
              HighLightedText(
                inputText = AnnotatedString(if (root.asJsonArray.isEmpty) "]" else "${root.toString().take(100)}.. ]"),
                overflow  = TextOverflow.Ellipsis,
                maxLines  = 1)
            }
          }
          if(fullPath !in collapsed) {
            run {
              val jsonArray      = root.asJsonArray
              val maxObjectCount = jsonArray.getMaxObjectCount(10000)

              jsonArray.forEachIndexed { index, item ->
                if (index >= maxObjectCount) {
                  SeeMoreText { setJsonArrayDialog(Pair(key ?: "Unknown key", jsonArray)) }
                  return@run
                } else {
                  JsonTree(item, null, "$fullPath[$index]", collapsed)
                }
              }
            }

            EndText("]")
          }
        }
      }
      root.isJsonObject -> {
        Column(modifier = Modifier.padding(horizontal = offset, vertical = 4.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            JsonText(
              key  = key,
              root = "{"
            )

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
                imageVector        = Icons.Default.ArrowDropDown,
                contentDescription = "Search",
                tint               = keyColor
              )

            if(fullPath in collapsed) {
              HighLightedText(
                inputText = AnnotatedString(if(root.asJsonObject.size() == 0) "}" else "${root.toString().take(100)}... }"),
                overflow  = TextOverflow.Ellipsis,
                maxLines  = 1)
            }
          }
          if(fullPath !in collapsed) {
            root.asJsonObject.entrySet().forEach { (key, value) ->
              JsonTree(value, key, fullPath, collapsed)
            }
            EndText("}")
          }
        }
      }
      else -> JsonText(modifier = Modifier.padding(horizontal = offset), key = key, root = "$root")
    }
  }

  jsonArrayDialog?.let {
    Dialog(
      onCloseRequest = {
        setSearch("")
        setJsonArrayDialog(null)
      },
      state = dialogState,
      title = "MockDog See more"
    ) {
      val lazyState                       = rememberLazyListState()
      val objectCollapsed                 = remember { mutableStateListOf<String>() }
      val (dialogSearch, setDialogSearch) = remember(jsonArrayDialog) { mutableStateOf("") }
      val matchesCount                    = remember {
        derivedStateOf {
          it.toString().getMatchCount(textToHighlight)
        }
      }

      val highlightItems by remember {
        derivedStateOf {
          val mutableList = mutableListOf<Pair<Int, Int>>()
          it.second.forEachIndexed { lazyItemIndex, jsonElement ->
            if (jsonElement.toString().toSearch().contains(textToHighlight.toSearch())) {
              repeat(jsonElement.toString().getMatchCount(textToHighlight)) { bringerIndex ->
                mutableList.add(Pair(lazyItemIndex, bringerIndex))
              }
            }
          }
          mutableList.toList()
        }
      }

      val (dialogSelected, setDialogSelected) = remember(highlightItems) { mutableStateOf(highlightItems.firstOrNull() ?: Pair(0, 0)) }   // first - lazyItemIndex, second - bringer index in bringerList

      LaunchedEffect(dialogSelected) {
        if (highlightItems.isNotEmpty()) {
          delay(10)
          val scrollTo = dialogSelected.first + 1
          if (lazyState.firstVisibleItemIndex != scrollTo) lazyState.scrollToItem(scrollTo)

          if (bringer.isNotEmpty() && (dialogSelected.second < bringer.size)) {
            delay(10)
            bringer[dialogSelected.second].bringIntoView()
          }
        }
      }

      Column(M.padding(8.dp)) {
        Text("Found ${matchesCount.value} matches in ${highlightItems.map { it.first }.toSet().size} list items.")
        val (dialogSelectedVal, setDialogSelectedVal) = remember(highlightItems) { mutableStateOf(0) }
        Row {
          searchView(
            search         = dialogSearch,
            setSearch      = { searching ->
              setDialogSearch(searching)
              setSearch(searching)
            },
            resultListSize = highlightItems.size,
            selected       = dialogSelectedVal,
            setSelected    = {
              setDialogSelected(highlightItems[it])
              setDialogSelectedVal(it)
            }
          )
        }

        Box(M.fillMaxWidth()) {
          SelectionContainer {
            LazyColumn(state = lazyState) {
              item {
                EndText("${it.first}: [")
              }
              itemsIndexed(it.second.asList()) { index, jsonElement ->
                JsonTree(root = jsonElement, null, "$index", objectCollapsed)
              }

              item {
                EndText("]")
              }
            }
          }

          VerticalScrollbar(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            adapter  = rememberScrollbarAdapter(lazyState)
          )
        }
      }
    }
  }

  Column(M.padding(horizontal = 16.dp).heightIn(max = dialogSize.height)) {
    if (showSearch) {
      Text(
        modifier = M.padding(vertical = 8.dp),
        text     = "Found $highlightCount matches in ${bringer.size} components.")
      searchView(
        search         = search,
        setSearch      = setSearch,
        resultListSize = bringer.size,
        selected       = selected,
        setSelected    = setSelected
      )

      LaunchedEffect(selected, highlightCount) {
        if (bringer.isNotEmpty()) {
          delay(10)
          bringer.elementAt(selected).bringIntoView()
        }
      }
    }

    val scrollState = rememberScrollState()

    Box {
      Column(M.verticalScroll(scrollState)) {
        if (jsonArrayDialog == null) {
          SelectionContainer {
            JsonTree(element, null, "", collapsed)
          }
        } else {
          EndText("Showing full list in another window in progress....")
        }
      }

      VerticalScrollbar(
        adapter  = rememberScrollbarAdapter(scrollState),
        modifier = M.align(Alignment.CenterEnd).fillMaxHeight()
      )
    }
  }
}

fun String.getMatchCount(textToMatch: String): Int {
  if (textToMatch.isEmpty()) return 0

  val matcher = Pattern.compile(textToMatch.toSearch()).matcher(this.toSearch())
  var count = 0
  while (matcher.find()) {
    count++
  }

  return count
}

private fun getIndexesToHighlight(
  inputText       : AnnotatedString,
  textToHighlight : String
) = if (textToHighlight.isNotBlank()) {
  val modifiedInputText = inputText.toSearch()

  modifiedInputText.mapIndexedNotNull { index, _ ->
    val founded = modifiedInputText.indexOf(textToHighlight.toSearch(), index)
    if (founded == -1) null else founded
  }.toSet()
} else setOf()

@Composable
private fun rememberHighlightText(
  inputText       : AnnotatedString,
  textToHighlight : String,
  highlightIndexes: Set<Int>
) = remember(textToHighlight, inputText) {
  buildAnnotatedString {
    append(inputText)
    highlightIndexes.map {
      addStyle(
        style = SpanStyle(background = Color.Yellow),
        start = it,
        end   = it + textToHighlight.length)
    }
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun searchView(
  search        : String,
  setSearch     : (String) -> Unit,
  resultListSize: Int,
  selected      : Int,
  setSelected   : (Int) -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  Row(
    modifier          = M.padding(bottom = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    BasicTextField(
      modifier          = M.height(40.dp).widthIn(min = 260.dp),
      value             = search,
      onValueChange     = setSearch,
      interactionSource = interactionSource,
    ) {
      TextFieldDefaults.OutlinedTextFieldDecorationBox(
        value                = search,
        visualTransformation = VisualTransformation.None,
        innerTextField       = it,
        singleLine           = false,
        enabled              = true,
        interactionSource    = interactionSource,
        contentPadding       = PaddingValues(12.dp),
        trailingIcon         = { IconButton(onClick = { setSearch("") }) { Icon(Icons.Filled.Close, null) }}
      )
    }

    Text(
      modifier = M.padding(start = 8.dp),
      text     = if (resultListSize < 1) "0 results" else "${selected + 1} / $resultListSize")

    IconButton(
      enabled = selected != 0,
      onClick = { setSelected(selected - 1) },
    ) {
      Icon(Icons.Filled.KeyboardArrowUp, null)
    }

    IconButton(
      enabled = selected < resultListSize - 1,
      onClick = { setSelected(selected + 1) }
    ) {
      Icon(Icons.Filled.KeyboardArrowDown, null)
    }
  }
}

@Composable
private fun EndText(text: String) = Text(text, color = PrimeBlack, fontSize = 14.sp)

private fun CharSequence.unaccented(): String =
  "\\p{InCombiningDiacriticalMarks}+".toRegex().replace(Normalizer.normalize(this, Normalizer.Form.NFD), "")

private fun  CharSequence.toSearch() = unaccented().lowercase()

private fun JsonArray.getMaxObjectCount(chunkSizeInBytes: Int = 1500): Int =
  max(1, chunkSizeInBytes / (this.toString().toByteArray().size / (size().takeIf { it > 0 } ?: 1)))

@Composable
private fun SeeMoreText(onClick: () -> Unit) {
  val text = buildAnnotatedString {
    withStyle(SpanStyle(color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
      append("...See more")
    }
  }
  DisableSelection {
    ClickableText( text = text, onClick = { onClick() })
  }
}