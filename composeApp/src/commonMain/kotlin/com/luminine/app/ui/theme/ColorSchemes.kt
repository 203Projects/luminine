package com.luminine.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.luminine.app.model.ThemeMode

// Light = the existing ivory/gold brand scheme.
val LightColorScheme: ColorScheme = lightColorScheme(
    primary = ReverseGold,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D9C2),
    onPrimaryContainer = ReverseEspresso,
    secondary = ReverseGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCECE6),
    onSecondaryContainer = Color(0xFF143D33),
    tertiary = ReverseCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3D5CC),
    onTertiaryContainer = Color(0xFF5B241A),
    background = ReverseIvory,
    onBackground = ReverseInk,
    surface = Color.White,
    onSurface = ReverseInk,
    surfaceVariant = Color(0xFFE9E1D7),
    onSurfaceVariant = Color(0xFF5C5249),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFECE3D8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF8F4),
    surfaceContainer = Color(0xFFF5EEE6),
    surfaceContainerHigh = Color(0xFFEDE4DA),
    surfaceContainerHighest = Color(0xFFE6DCD1),
    surfaceTint = ReverseGold,
    inverseSurface = ReverseEspresso,
    inverseOnSurface = ReverseIvory,
    inversePrimary = Color(0xFFD5BE9A),
    outline = Color(0xFFB8A99A),
    outlineVariant = Color(0xFFD8CABC),
)

// Dark = warm espresso surfaces, gold accents.
val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFD5BE9A),
    onPrimary = Color(0xFF3A2C1E),
    primaryContainer = Color(0xFF5B4632),
    onPrimaryContainer = Color(0xFFF1E5D2),
    secondary = Color(0xFF8FCBBA),
    onSecondary = Color(0xFF0E3329),
    secondaryContainer = Color(0xFF234B40),
    onSecondaryContainer = Color(0xFFD4ECE4),
    tertiary = Color(0xFFE9A593),
    onTertiary = Color(0xFF54231A),
    background = Color(0xFF1A1714),
    onBackground = Color(0xFFEDE4DA),
    surface = Color(0xFF211D19),
    onSurface = Color(0xFFEDE4DA),
    surfaceVariant = Color(0xFF463F38),
    onSurfaceVariant = Color(0xFFD0C5B8),
    surfaceContainerLowest = Color(0xFF15120F),
    surfaceContainerLow = Color(0xFF211D19),
    surfaceContainer = Color(0xFF262220),
    surfaceContainerHigh = Color(0xFF312C28),
    surfaceContainerHighest = Color(0xFF3C3733),
    surfaceTint = Color(0xFFD5BE9A),
    outline = Color(0xFF978A7C),
    outlineVariant = Color(0xFF4C453E),
)

// High contrast = pure black on white, max-contrast borders. Independent of light/dark.
val HighContrastColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF000000),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFF00332B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF000000),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6A0F00),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F2F2),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    outline = Color(0xFF000000),
    outlineVariant = Color(0xFF000000),
)

// Pure resolver — high contrast overrides everything; System follows the OS dark flag.
fun resolveColorScheme(mode: ThemeMode, highContrast: Boolean, systemDark: Boolean): ColorScheme {
    if (highContrast) return HighContrastColorScheme
    return when (mode) {
        ThemeMode.Light -> LightColorScheme
        ThemeMode.Dark -> DarkColorScheme
        ThemeMode.System -> if (systemDark) DarkColorScheme else LightColorScheme
    }
}
