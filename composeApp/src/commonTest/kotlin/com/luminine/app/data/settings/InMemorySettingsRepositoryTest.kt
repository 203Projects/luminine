package com.luminine.app.data.settings

import com.luminine.app.model.FontScale
import com.luminine.app.model.LuminineSettings
import com.luminine.app.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemorySettingsRepositoryTest {
    @Test
    fun observeStartsWithDefaults() = runTest {
        assertEquals(LuminineSettings(), InMemorySettingsRepository().observe().first())
    }

    @Test
    fun updateMutatesAndEmits() = runTest {
        val repo = InMemorySettingsRepository()
        repo.update { it.copy(themeMode = ThemeMode.Dark, fontScale = FontScale.Large) }
        val s = repo.observe().first()
        assertEquals(ThemeMode.Dark, s.themeMode)
        assertEquals(FontScale.Large, s.fontScale)
    }

    @Test
    fun updateComposesOverPriorState() = runTest {
        val repo = InMemorySettingsRepository()
        repo.update { it.copy(highContrast = true) }
        repo.update { it.copy(fontScale = FontScale.ExtraLarge) }
        val s = repo.observe().first()
        assertEquals(true, s.highContrast)
        assertEquals(FontScale.ExtraLarge, s.fontScale)
    }
}
