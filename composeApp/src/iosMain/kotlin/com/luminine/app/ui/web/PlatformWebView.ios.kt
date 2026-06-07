package com.luminine.app.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.luminine.app.model.FontScale
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKWebView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformWebView(
    url: String,
    readingMode: Boolean,
    fontScale: FontScale,
    controller: WebViewController,
    modifier: Modifier,
) {
    val state = remember { IosWebViewState() }
    UIKitView(
        modifier = modifier,
        factory = {
            val web = WKWebView()
            controller.bind(
                back = { if (web.canGoBack) web.goBack() },
                forward = { if (web.canGoForward) web.goForward() },
                reload = { web.reload() },
            )
            NSURL.URLWithString(url)?.let { web.loadRequest(NSURLRequest(it)) }
            web
        },
        update = { web ->
            controller.onNavStateChanged(web.canGoBack, web.canGoForward)
            // Only act on an actual reading-mode TRANSITION — `update` fires on every recomposition,
            // so reacting to the level alone would re-inject or reload on ordinary browsing.
            if (readingMode != state.lastReadingMode) {
                if (readingMode) {
                    web.evaluateJavaScript(readerInjectionJs(fontScale), null)
                } else {
                    web.reload()
                }
                state.lastReadingMode = readingMode
            }
        },
    )
}

private class IosWebViewState {
    var lastReadingMode: Boolean = false
}

actual fun openInExternalBrowser(url: String) {
    NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
}
