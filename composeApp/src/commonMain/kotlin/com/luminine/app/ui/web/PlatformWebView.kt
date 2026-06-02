package com.luminine.app.ui.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.luminine.app.model.FontScale

// Common controller shared by both platform WebViews. The actual WebView reports history changes
// via onNavStateChanged and binds its native back/forward/reload to the action handlers.
class WebViewController {
    var canGoBack by mutableStateOf(false)
        private set
    var canGoForward by mutableStateOf(false)
        private set

    private var back: () -> Unit = {}
    private var forward: () -> Unit = {}
    private var doReload: () -> Unit = {}

    fun onNavStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        this.canGoBack = canGoBack
        this.canGoForward = canGoForward
    }

    fun bind(back: () -> Unit, forward: () -> Unit, reload: () -> Unit) {
        this.back = back
        this.forward = forward
        this.doReload = reload
    }

    fun goBack() = back()
    fun goForward() = forward()
    fun reload() = doReload()
}

// Reader stylesheet injected into the live page in reading mode. Base font honors the app font-scale
// so reading mode + the global font setting compose. Pure + testable.
fun readerCss(fontScale: FontScale): String {
    val basePx = (18 * fontScale.multiplier).toInt()
    return """
        header, footer, nav, aside, .ad, .ads { display:none !important; }
        body { max-width: 720px; margin: 0 auto; padding: 16px;
               line-height: 1.9 !important; font-size: ${basePx}px !important; }
        p, li { line-height: 1.9 !important; }
        img { max-width: 100%; height: auto; }
    """.trimIndent()
}

// JS that injects readerCss into the live page. Single source of truth for both platform actuals.
// XSS-safe by construction: the CSS is app-controlled and added via createTextNode (NOT innerHTML),
// and JsonPrimitive(...).toString() emits a fully-escaped, quoted JS string literal. Pure + testable.
fun readerInjectionJs(fontScale: FontScale): String {
    val cssLiteral = kotlinx.serialization.json.JsonPrimitive(readerCss(fontScale)).toString()
    return "(function(){var s=document.createElement('style');" +
        "s.appendChild(document.createTextNode($cssLiteral));" +
        "document.head.appendChild(s);})();"
}

// Renders a native WebView. readingMode toggles reader-CSS injection. controller carries nav state.
@Composable
expect fun PlatformWebView(
    url: String,
    readingMode: Boolean,
    controller: WebViewController,
    modifier: Modifier = Modifier,
)

// Opens the URL in the device's default browser (leaves the app).
expect fun openInExternalBrowser(url: String)
