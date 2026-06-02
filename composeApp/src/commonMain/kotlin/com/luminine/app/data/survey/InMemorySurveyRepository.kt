package com.luminine.app.data.survey

import com.luminine.app.model.SurveyResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Suspend-correct, thread-safe in-memory survey store; also the compile-safe default used by
// LuminineDependencies before platform startup overrides it with a DataStore-backed repo.
class InMemorySurveyRepository(initial: SurveyResponse? = null) : SurveyRepository {
    private val mutex = Mutex()
    private var current: SurveyResponse? = initial

    override suspend fun save(response: SurveyResponse) = mutex.withLock { current = response }
    override suspend fun load(): SurveyResponse? = mutex.withLock { current }
    override suspend fun clear() = mutex.withLock { current = null }
}
