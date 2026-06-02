package com.luminine.app.ui.web

import com.luminine.app.model.FontScale
import kotlin.test.Test
import kotlin.test.assertTrue

class ReaderCssTest {
    @Test
    fun containsWidenedLineHeightAndHidesChrome() {
        val css = readerCss(FontScale.Normal)
        assertTrue("line-height" in css)
        assertTrue("display:none" in css.replace(" ", ""))
    }

    @Test
    fun fontScaleAffectsBaseFontSize() {
        val small = readerCss(FontScale.Small)
        val large = readerCss(FontScale.ExtraLarge)
        fun px(s: String) = Regex("font-size:\\s*(\\d+)px").find(s)!!.groupValues[1].toInt()
        assertTrue(px(large) > px(small))
    }

    @Test
    fun injectionUsesCreateTextNodeNotInnerHtml() {
        val js = readerInjectionJs(FontScale.Normal)
        assertTrue("createTextNode" in js)
        assertTrue("innerHTML" !in js)
    }
}
