package com.luminine.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminine.app.ui.LuminineIcon
import com.luminine.app.ui.components.LuminineIconView
import com.luminine.app.ui.web.PlatformWebView
import com.luminine.app.ui.web.WebViewController
import com.luminine.app.ui.web.openInExternalBrowser

@Composable
fun WebViewScreen(url: String, title: String, onClose: () -> Unit) {
    val controller = remember { WebViewController() }
    var readingMode by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("닫기") }
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            PlatformWebView(
                url = url,
                readingMode = readingMode,
                controller = controller,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { controller.goBack() }, enabled = controller.canGoBack) {
                    LuminineIconView(LuminineIcon.Link, "뒤로", Modifier.size(20.dp))
                }
                IconButton(onClick = { controller.goForward() }, enabled = controller.canGoForward) {
                    LuminineIconView(LuminineIcon.Link, "앞으로", Modifier.size(20.dp))
                }
                IconButton(onClick = { controller.reload() }) {
                    LuminineIconView(LuminineIcon.Link, "새로고침", Modifier.size(20.dp))
                }
                IconButton(onClick = { openInExternalBrowser(url) }) {
                    LuminineIconView(LuminineIcon.Link, "외부 브라우저로 열기", Modifier.size(20.dp))
                }
                FilledTonalButton(onClick = { readingMode = !readingMode }) {
                    Text(if (readingMode) "읽기 모드 끄기" else "읽기 모드")
                }
            }
        }
    }
}
