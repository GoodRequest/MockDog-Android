package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Send
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mockdog.*
import okhttp3.internal.closeQuietly
import theme.*
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*

//private val submitted         = mutableStateOf(false) // TODO co to?
//private val deviceCheckbox    = mutableStateOf(false)
private val throttleCheckbox = mutableStateOf(false)

@Composable
fun LeftPane(listWidth: Dp, selected: UUID?, onSelect: (UUID) -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(M.width(listWidth).fillMaxHeight().background(Color.White)) {
        Column(M.weight(1f)) {
            Row(
                modifier = M.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(modifier = M.padding(16.dp),text = "History", style = T.subtitle1)
                if (requests.isNotEmpty())
                    Button(
                        modifier = M.padding(end = 16.dp),
                        onClick = {
                            requests.clear()
                        }
                    ){
                        Text(text = "Clear History")
                    }
            }

            RequestHistory(requests, selected, onSelect)
        }

        Divider(M.height(1.dp).padding(horizontal = 16.dp))
        Servers()
        Divider(M.height(1.dp).padding(horizontal = 16.dp))
        Text(modifier = M.padding(16.dp), text = "Settings", style = T.subtitle1)
        LeftPaneRow("Mock responses", catchEnabled.value) { catchEnabled.value = catchEnabled.value.not() }
        // LeftPaneRow("Test on device", deviceCheckbox.value) { deviceCheckbox.value = deviceCheckbox.value.not() }
        LeftPaneRow("Throttle requests", throttleCheckbox.value) {
            throttleCheckbox.value = throttleCheckbox.value.not()
        }
        if (throttleCheckbox.value) ThrottleRequestSimulation(modifier = M.padding(horizontal = 16.dp).width(listWidth))

//    if (deviceCheckbox.value) {
//      val (ip, setIp)       = mutable("")
//      val (ipErr, setIpErr) = mutable(false)
//
//      if(ipErr) { ErrorText("Zadaj validnú IP") }
//
//      Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = M.width(listWidth).padding(horizontal = 16.dp)
//      ) {
//        OutlinedTextField(
//          maxLines      = 1,
//          label         = { Text("Zadaj IP", M.padding(top = 4.dp)) },
//          colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
//          isError       = ipErr,
//          value         = ip,
//          onValueChange = { setIp(it); setIpErr(false) })
//
//        Button(
//          modifier = M.padding(top = 10.dp),
//          onClick  = {
//            when {
//              ip.isBlank()                    -> setIpErr(true)
//              requests.size != responses.size -> scope.launch { snackbar.showSnackbar("Set responses to all requests") }
//              else -> {
//                startServer(inetAddress = InetAddress.getByName(ip), port = 52242)
//                submitted.value = true
//                scope.launch { snackbar.showSnackbar("Device mode") }
//              }
//            }
//          }
//        ) { Text(modifier = M.padding(horizontal = 16.dp), text = "Submit") }
//      }
//    } else if (submitted.value) {
//      if (requests.size != responses.size) {
//        deviceCheckbox.value = true
//        scope.launch { snackbar.showSnackbar("Set responses to all requests") }
//      } else {
//        startServer()
//        submitted.value = false
//        scope.launch { snackbar.showSnackbar("Emulator mode") }
//      }
//    }

        SnackbarHost(snackbar)
    }
}

@Composable
fun RequestHistory(requests: SnapshotStateList<Request>, selected: UUID?, onSelect: (UUID) -> Unit) {
    val lazyListState = rememberLazyListState()

    LaunchedEffect(requests.lastOrNull()) {
        if (requests.isNotEmpty())
            lazyListState.animateScrollToItem(requests.indices.last)
    }

    if (requests.isEmpty())
        Text("no requests yet", M.padding(horizontal = 16.dp))



    LazyColumn(M.background(Color.White), reverseLayout = true, state = lazyListState) {
        itemsIndexed(requests) { index, request ->
            val isSelected = (request.id == selected)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = M
                    .fillParentMaxWidth()
                    .background(
                        when {
                            isSelected -> BlueLight
                            index % 2 == 1 -> C.surface
                            else -> C.background
                        }
                    )
                    .clickable { onSelect(request.id) }
                    .padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(modifier = M.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween,modifier = M.fillMaxWidth()) {
                        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                        Text(
                            text = sdf.format(Date(request.timeStamp)),
                            style = T.overline,

                        )
                        Text(
                            text = request.request.getHeader("Host").toString(),
                            style = T.overline,
                            modifier = M.padding(start = 16.dp)
                        )
                    }
                        Row(horizontalArrangement = Arrangement.SpaceBetween,modifier = M.fillMaxWidth()) {
                            Row(M.width(40.dp)) {
                                when (val response = responses[request.id]) {
                                    null -> Icon(
                                        modifier = M.size(22.dp).clickable { sendRealResponse(request.id) },
                                        imageVector = Icons.Rounded.Send,
                                        tint = C.onBackground,
                                        contentDescription = ""
                                    )

                                    is Loading -> Text(
                                        text = "...",
                                        color = C.onBackground,
                                        style = T.body2
                                    )

                                    is SentResponse -> Text(
                                        text = response.status.toString(),
                                        color = C.onBackground,
                                        style = T.body2
                                    )
                                }
                            }
                            Text(
                                modifier = M.width(50.dp),
                                text = request.request.method.toString(),
                                color = C.onBackground,
                                style = T.body2
                            )
                            Text(
                                text = request.request.path!!,
                                style = T.body2
                            )
                        }
                }
            }
        }
    }
}

@Composable
fun LeftPaneRow(text: String, check: Boolean, onCheck: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = check, onCheckedChange = onCheck)
        Text(style = T.body2, text = text)
    }
}

@Composable
fun Servers() {

    Column {
        Text(modifier = M.padding(16.dp), text = "Servers", style = T.subtitle1)
        LazyColumn {
            items(servers) { server ->
                var buttonColor = remember { mutableStateOf(Red) }
                var buttonText = remember { mutableStateOf("OFF") }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        style = T.button,
                        text = server.server?.hostName ?: "Unknown",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Button(
                        onClick = {
                            server.server.shutdown()
                            server.server.close()
                            server.server.closeQuietly()
                            server.running = false
                            servers.remove(server)
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Red),
                        modifier = Modifier.padding(horizontal = 16.dp).defaultMinSize(minWidth = 100.dp)
                    ) {
                        Text(style = T.button, text = "DELETE")
                    }
                }
            }
        }
        val ipInput = remember { mutableStateOf("") }
        Divider(M.height(1.dp).padding(horizontal = 16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {

            OutlinedTextField(
                modifier      = M.padding(bottom = 16.dp, start = 16.dp).weight(1f),
                maxLines      = 15,
                label         = { Text("IP Address", M.padding(top = 4.dp), style = T.caption) },
                colors        = TextFieldDefaults.outlinedTextFieldColors(backgroundColor = C.surface),
                value         = ipInput.value,
                onValueChange = { ipInput.value = it })
            Button(
                onClick = {
                    addAndStartServer(inetAddress = InetAddress.getByName(ipInput.value))
                    ipInput.value = ""
                }, colors = ButtonDefaults.buttonColors(backgroundColor = GreenLight),
                modifier = Modifier.padding(horizontal = 16.dp).defaultMinSize(minWidth = 100.dp)
            ) {
                Text(style = T.button, text = "ADD")
            }
        }

    }

}

@Composable
fun ThrottleRequestSimulation(modifier: Modifier) {
    val (bytes, setBytes) = mutable(0f)
    val (period, setPeriod) = mutable(0f)
    val data = bytes.toLong()
    val time = period.toLong()

    bytesPerPeriod.value = data
    timeInMillis.value = time

    DogBodyText(modifier, "Bytes per period: ${if (data == 0L) "0 bytes" else "$data bytes"}")
    DogSlider(modifier, bytes, setBytes)

    DogBodyText(modifier, "Sleep after bytes: ${if (time == null) "0 milis" else "$time milis"}") //TODO
    DogSlider(modifier, period, setPeriod)
}

@Composable
fun DogSlider(modifier: Modifier, value: Float?, setValue: (Float) -> Unit) {
    Slider(
        modifier = modifier,
        value = value ?: 0.0f,
        onValueChange = setValue,
        valueRange = 0f..100f,
        steps = 99
    )
}

@Composable
fun DogBodyText(modifier: Modifier, text: String) {
    Text(
        modifier = modifier,
        style = T.body2,
        text = text
    )
}