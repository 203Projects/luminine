package com.luminine.app.di

import com.luminine.app.auth.KakaoAuthClient
import com.luminine.app.auth.StubKakaoAuthClient
import com.luminine.app.data.session.InMemorySessionRepository
import com.luminine.app.data.session.SessionRepository
import com.luminine.app.data.store.DataStoreSessionRepository
import com.luminine.app.data.store.DataStoreSurveyRepository
import com.luminine.app.data.store.createLuminineDataStore
import com.luminine.app.data.survey.InMemorySurveyRepository
import com.luminine.app.data.survey.SurveyRepository
import kotlinx.serialization.json.Json

// Shared Json config for DataStore persistence. encodeDefaults=true so skippable survey sections
// persist their defaults; ignoreUnknownKeys=true for forward-compat as the schema grows.
val LuminineJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Global, install-once dependency holder. Defaults to IN-MEMORY so App()/MainViewController()
 * (both no-arg) always compile and run even if DataStore is never initialized. Platform startup
 * calls installDataStore() to swap in restart-surviving repositories. Persistence is OPT-IN;
 * in-memory is the zero-risk default — if DataStore wiring is skipped, nothing crashes.
 */
object LuminineDependencies {
    var kakaoAuthClient: KakaoAuthClient = StubKakaoAuthClient()
        private set
    var sessionRepository: SessionRepository = InMemorySessionRepository()
        private set
    var surveyRepository: SurveyRepository = InMemorySurveyRepository()
        private set

    private var dataStoreInstalled = false

    /**
     * Install DataStore-backed repositories. Idempotent: a second call is a no-op, so the iOS
     * single-DataStore-instance rule cannot be violated (a second instance on the same file path
     * would throw at runtime). On Android, call only after AndroidAppContext is seeded.
     */
    fun installDataStore() {
        if (dataStoreInstalled) return
        val store = createLuminineDataStore()
        sessionRepository = DataStoreSessionRepository(store)
        surveyRepository = DataStoreSurveyRepository(store)
        dataStoreInstalled = true
    }

    /** Test/override hook — lets tests inject in-memory or fake repositories. */
    fun override(
        kakao: KakaoAuthClient = kakaoAuthClient,
        session: SessionRepository = sessionRepository,
        survey: SurveyRepository = surveyRepository,
    ) {
        kakaoAuthClient = kakao
        sessionRepository = session
        surveyRepository = survey
    }
}
