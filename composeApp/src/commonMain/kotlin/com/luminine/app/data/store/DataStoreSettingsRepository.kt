package com.luminine.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luminine.app.data.settings.SettingsRepository
import com.luminine.app.di.LuminineJson
import com.luminine.app.model.LuminineSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = LuminineJson,
) : SettingsRepository {
    private val key = stringPreferencesKey("settings_json")

    override fun observe(): Flow<LuminineSettings> = dataStore.data.map { prefs ->
        decode(prefs[key])
    }

    override suspend fun update(transform: (LuminineSettings) -> LuminineSettings) {
        dataStore.edit { prefs ->
            val current = decode(prefs[key])
            prefs[key] = json.encodeToString(LuminineSettings.serializer(), transform(current))
        }
    }

    // Corrupt/old JSON falls back to defaults, never crashes (matches DataStoreSessionRepository).
    private fun decode(raw: String?): LuminineSettings =
        raw?.let { runCatching { json.decodeFromString(LuminineSettings.serializer(), it) }.getOrNull() }
            ?: LuminineSettings()
}
