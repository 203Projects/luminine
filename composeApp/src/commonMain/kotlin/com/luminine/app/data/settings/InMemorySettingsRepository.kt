package com.luminine.app.data.settings

import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// Compile-safe default used by LuminineDependencies before platform startup swaps in DataStore.
class InMemorySettingsRepository(initial: LuminineSettings = LuminineSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override fun observe(): Flow<LuminineSettings> = state
    override suspend fun update(transform: (LuminineSettings) -> LuminineSettings) {
        state.update(transform)
    }
}
