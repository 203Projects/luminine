package com.luminine.app.model

import com.luminine.app.di.LuminineJson
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsTest {
    @Test
    fun defaultsAreSystemNoContrastNormal() {
        val s = LuminineSettings()
        assertEquals(ThemeMode.System, s.themeMode)
        assertEquals(false, s.highContrast)
        assertEquals(FontScale.Normal, s.fontScale)
    }

    @Test
    fun fontScaleMultipliers() {
        assertEquals(0.85f, FontScale.Small.multiplier)
        assertEquals(1.0f, FontScale.Normal.multiplier)
        assertEquals(1.25f, FontScale.Large.multiplier)
        assertEquals(1.5f, FontScale.ExtraLarge.multiplier)
    }

    @Test
    fun jsonRoundTrips() {
        val s = LuminineSettings(ThemeMode.Dark, highContrast = true, fontScale = FontScale.Large)
        val raw = LuminineJson.encodeToString(LuminineSettings.serializer(), s)
        assertEquals(s, LuminineJson.decodeFromString(LuminineSettings.serializer(), raw))
    }
}
