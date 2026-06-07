package com.luminine.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luminine.app.data.session.Session
import com.luminine.app.data.session.SessionRepository
import com.luminine.app.di.LuminineJson
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DataStoreSessionRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = LuminineJson,
) : SessionRepository {
    private val key = stringPreferencesKey("session_json")

    override suspend fun save(session: Session) {
        dataStore.edit { it[key] = json.encodeToString(Session.serializer(), session) }
    }

    override suspend fun load(): Session? {
        val raw = dataStore.data.first()[key] ?: return null
        // Guard against corrupt/old JSON: treat as logged out, never crash.
        return runCatching { json.decodeFromString(Session.serializer(), raw) }.getOrNull()
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
