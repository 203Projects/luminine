package com.luminine.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.luminine.app.model.LuminineSettings

// Brand color tokens (schemes live in ColorSchemes.kt).
val ReverseIvory = Color(0xFFF7F3EE)
val ReverseGold = Color(0xFF8A7355)
val ReverseEspresso = Color(0xFF4B3628)
val ReverseGreen = Color(0xFF2D7D68)
val ReverseCoral = Color(0xFFC05A47)
val ReverseInk = Color(0xFF24211F)

@Composable
fun LuminineTheme(
    settings: LuminineSettings = LuminineSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val scheme = resolveColorScheme(settings.themeMode, settings.highContrast, systemDark)
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale * settings.fontScale.multiplier,
    )
    MaterialTheme(
        colorScheme = scheme,
        typography = MaterialTheme.typography,
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
    }
}
