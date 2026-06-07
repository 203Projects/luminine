package com.luminine.app.model

import kotlinx.serialization.Serializable

enum class ThemeMode { System, Light, Dark }

enum class FontScale(val multiplier: Float) {
    Small(0.85f),
    Normal(1.0f),
    Large(1.25f),
    ExtraLarge(1.5f),
}

@Serializable
data class LuminineSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val highContrast: Boolean = false,
    val fontScale: FontScale = FontScale.Normal,
)
