package com.luminine.app.ui.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WebViewControllerTest {
    @Test
    fun startsWithNoHistory() {
        val c = WebViewController()
        assertFalse(c.canGoBack)
        assertFalse(c.canGoForward)
    }

    @Test
    fun navStateReflectsLastReportedValues() {
        val c = WebViewController()
        c.onNavStateChanged(canGoBack = true, canGoForward = false)
        assertTrue(c.canGoBack)
        assertFalse(c.canGoForward)
    }

    @Test
    fun actionsInvokeBoundHandlers() {
        val c = WebViewController()
        val calls = mutableListOf<String>()
        c.bind(back = { calls += "back" }, forward = { calls += "forward" }, reload = { calls += "reload" })
        c.goBack(); c.goForward(); c.reload()
        assertEquals(listOf("back", "forward", "reload"), calls)
    }

    @Test
    fun actionsAreNoOpsBeforeBinding() {
        WebViewController().goBack() // must not throw
    }
}
