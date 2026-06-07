package com.luminine.app.ui.theme

import com.luminine.app.model.ThemeMode
import kotlin.test.Test
import kotlin.test.assertSame

class ResolveColorSchemeTest {
    @Test
    fun highContrastWinsRegardlessOfMode() {
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.Light, highContrast = true, systemDark = false))
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.Dark, highContrast = true, systemDark = true))
        assertSame(HighContrastColorScheme, resolveColorScheme(ThemeMode.System, highContrast = true, systemDark = true))
    }

    @Test
    fun lightAndDarkModesAreExplicit() {
        assertSame(LightColorScheme, resolveColorScheme(ThemeMode.Light, highContrast = false, systemDark = true))
        assertSame(DarkColorScheme, resolveColorScheme(ThemeMode.Dark, highContrast = false, systemDark = false))
    }

    @Test
    fun systemModeFollowsSystemDark() {
        assertSame(DarkColorScheme, resolveColorScheme(ThemeMode.System, highContrast = false, systemDark = true))
        assertSame(LightColorScheme, resolveColorScheme(ThemeMode.System, highContrast = false, systemDark = false))
    }
}
