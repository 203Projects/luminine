package com.luminine.app.data.survey

import com.luminine.app.model.SurveyResponse

// Persists the single user's onboarding survey. load() returns null before the survey is saved.
interface SurveyRepository {
    suspend fun save(response: SurveyResponse)
    suspend fun load(): SurveyResponse?
    suspend fun clear()
}
