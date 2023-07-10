package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import core.*
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jetbrains.skia.Image
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JDialog
import javax.swing.JLabel

@Composable
fun rememberPainter(resource: String) = remember(resource) {
  requireNotNull(
    value       = runCatching { File("src/main/kotlin/resources", resource) }.getOrNull(),
    lazyMessage = { "Resource src/main/kotlin/resources/$resource not found"}
  ).inputStream().use { inputStream ->
    Image.makeFromEncoded(inputStream.readAllBytes())
  }.toComposeImageBitmap()
}

@Composable
fun PushDogDialog(
  visible       : Boolean,
  onCloseRequest: () -> Unit,
) {
  val windowsListener = remember {
    object: WindowAdapter() {
      override fun windowClosing(e: WindowEvent?) {
        onCloseRequest()
      }
    }
  }

  val imageIcon = rememberPainter("pushdog.png").toAwtImage()

  Dialog(
    visible = visible,
    create  = { ComposeDialog().apply {
      setIconImage(imageIcon)

      title = "PushDog"
      setSize(800, 800)
      defaultCloseOperation = JDialog.DO_NOTHING_ON_CLOSE
      addWindowListener(windowsListener)
    } },
    dispose = {
      it.removeWindowListener(windowsListener)
      it.dispose()
    }
  ) {
    PushDogScreen()
  }
}
@Composable
private fun PushDogScreen() {
  val server = mutable(fcmToken.value)
  val device = mutable("")
  val body   = mutable("")

  LaunchedEffect(fcmToken.value) {
    server.value = fcmToken.value
  }

  val (response, setResponse) = mutable<ApiResponse?>(null)

  Column(modifier = M.padding(16.dp)) {
    Column(
      modifier            = M.verticalScroll(rememberScrollState()).weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text       = "I'm not classic dog, I am PushDog! Woff",
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp)
      OutlinedTextField(
        modifier      = M.fillMaxWidth(),
        label         = { Text("Server Key") },
        placeholder   = { Text("e.g. -> AAAAR-COoB4:APA91bFZ6O2Z7Iesqbzrn3lZ9J6u15JLfFMDbqXkonuXXJlbklUdDizLdgs3WfU10ojOr5q2XNm51auXpzNrjb6WLz") },
        value         = server.value,
        onValueChange = { server.value = it })

      OutlinedTextField(
        modifier      = M.fillMaxWidth(),
        label         = { Text("Device FCM Token") },
        placeholder   = { Text("e.g. -> duOvkjSRTqSO8e13vYJlIY:APA91bEoEsvFv_RR7nzArRKbjH00e67xC4zIO5vDaZhe-F0NNQq155pbMKVS") },
        value         = device.value,
        onValueChange = { device.value = it })

      OutlinedTextField(
        modifier      = M.fillMaxWidth(),
        label         = { Text("Body") },
        value         = body.value,
        onValueChange = { body.value = it })

      Button(
        content = {
          if (response is ApiResponse.Loading)
            CircularProgressIndicator(color = C.onBackground, modifier = M.size(20.dp))
          else
            Text("Send FCM notification")
        },
        enabled = server.value.isNotEmpty() && device.value.isNotEmpty() && body.value.isNotEmpty() && response !is ApiResponse.Loading,
        onClick = {
          POST(
            client   = client,
            path     = "https://fcm.googleapis.com/fcm/send".toHttpUrl(),
            headers  = Headers.headersOf("Authorization", "key=${server.value}"),
            body     = jsonBody("""{"data": ${body.value},"to": "${device.value}"}""".trimIndent()),
            response = setResponse)
        })
    }

    if (response is ApiResponse.Content) {
      Text(
        modifier   = M.padding(top = 16.dp).fillMaxWidth(),
        text       = "Response from FCM Google API:",
        textAlign  = TextAlign.Start,
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp)

      SwingPanel(
        modifier = M.width(800.dp).height(150.dp),
        factory  = { JLabel(response.body) })
    }
  }
}