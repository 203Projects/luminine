package com.luminine.app.ui.web

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.luminine.app.model.FontScale
import com.luminine.app.platform.AndroidAppContext

@Composable
actual fun PlatformWebView(
    url: String,
    readingMode: Boolean,
    fontScale: FontScale,
    controller: WebViewController,
    modifier: Modifier,
) {
    val context = LocalContext.current
    // Holders so the once-built WebViewClient reads the LATEST reading-mode/font-scale on each page
    // load (the factory closure would otherwise capture stale params across recompositions).
    val state = remember { AndroidWebViewState() }
    state.readingMode = readingMode
    state.fontScale = fontScale
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, finishedUrl: String?) {
                        controller.onNavStateChanged(view.canGoBack(), view.canGoForward())
                        if (state.readingMode) view.evaluateJavascript(readerInjectionJs(state.fontScale), null)
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
            // Only act on an actual reading-mode TRANSITION — `update` fires on every recomposition
            // (e.g. when nav-state changes), so reacting to the level alone would re-inject or reload
            // the page on ordinary browsing. Re-injection after in-mode navigations is handled in
            // onPageFinished.
            if (readingMode != state.lastReadingMode) {
                if (readingMode) view.evaluateJavascript(readerInjectionJs(fontScale), null) else view.reload()
                state.lastReadingMode = readingMode
            }
        },
    )
}

private class AndroidWebViewState {
    var readingMode: Boolean = false
    var fontScale: FontScale = FontScale.Normal
    var lastReadingMode: Boolean = false
}

actual fun openInExternalBrowser(url: String) {
    val ctx = AndroidAppContext.require()
    ctx.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
