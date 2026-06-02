package com.luminine.app.ui.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.luminine.app.model.FontScale
import com.luminine.app.platform.AndroidAppContext

@Composable
actual fun PlatformWebView(url: String, readingMode: Boolean, controller: WebViewController, modifier: Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        controller.onNavStateChanged(view.canGoBack(), view.canGoForward())
                        if (readingMode) view.evaluateJavascript(readerInjectionJs(FontScale.Normal), null)
                    }
                }
                controller.bind(
                    back = { if (canGoBack()) goBack() },
                    forward = { if (canGoForward()) goForward() },
                    reload = { reload() },
                )
                loadUrl(url)
            }
        },
        update = { view ->
            if (readingMode) view.evaluateJavascript(readerInjectionJs(FontScale.Normal), null)
        },
    )
}

actual fun openInExternalBrowser(url: String) {
    val ctx = AndroidAppContext.require()
    ctx.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
