package com.reverse.healthtracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ReverseIvory = Color(0xFFF7F3EE)
val ReverseGold = Color(0xFF8A7355)
val ReverseEspresso = Color(0xFF4B3628)
val ReverseGreen = Color(0xFF2D7D68)
val ReverseCoral = Color(0xFFC05A47)
val ReverseInk = Color(0xFF24211F)

private val reverseColorScheme: ColorScheme = lightColorScheme(
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

@Composable
fun ReverseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = reverseColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
