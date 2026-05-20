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
    secondary = ReverseGreen,
    onSecondary = Color.White,
    tertiary = ReverseCoral,
    background = ReverseIvory,
    onBackground = ReverseInk,
    surface = Color.White,
    onSurface = ReverseInk,
    surfaceVariant = Color(0xFFE9E1D7),
    onSurfaceVariant = Color(0xFF5C5249),
    outline = Color(0xFFB8A99A),
)

@Composable
fun ReverseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = reverseColorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
