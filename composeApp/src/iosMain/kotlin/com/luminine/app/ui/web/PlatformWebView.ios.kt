package com.luminine.app.ui.web

import androidx.compose.runtime.Composable
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
            // Inject reader CSS at the current font scale when on; reload to drop it when turned off.
            if (readingMode) {
                web.evaluateJavaScript(readerInjectionJs(fontScale), null)
            } else {
                web.reload()
            }
        },
    )
}

actual fun openInExternalBrowser(url: String) {
    NSURL.URLWithString(url)?.let { UIApplication.sharedApplication.openURL(it) }
}
