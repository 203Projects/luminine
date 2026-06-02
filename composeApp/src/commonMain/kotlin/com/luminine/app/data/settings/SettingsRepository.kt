package com.luminine.app.data.settings

import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow

// Reactive read so theme/font changes recompose the whole app live. update() applies a transform
// over the latest value (read-modify-write), mirroring DataStore's edit{} semantics.
interface SettingsRepository {
    fun observe(): Flow<LuminineSettings>
    suspend fun update(transform: (LuminineSettings) -> LuminineSettings)
}
