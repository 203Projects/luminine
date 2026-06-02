package com.luminine.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.luminine.app.data.survey.SurveyRepository
import com.luminine.app.di.LuminineJson
import com.luminine.app.model.SurveyResponse
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

class DataStoreSurveyRepository(
    private val dataStore: DataStore<Preferences>,
    private val json: Json = LuminineJson,
) : SurveyRepository {
    private val key = stringPreferencesKey("survey_json")

    override suspend fun save(response: SurveyResponse) {
        dataStore.edit { it[key] = json.encodeToString(SurveyResponse.serializer(), response) }
    }

    override suspend fun load(): SurveyResponse? {
        val raw = dataStore.data.first()[key] ?: return null
        // runCatching makes partial/skippable-section JSON evolution forward-tolerant.
        return runCatching { json.decodeFromString(SurveyResponse.serializer(), raw) }.getOrNull()
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(key) }
    }
}
