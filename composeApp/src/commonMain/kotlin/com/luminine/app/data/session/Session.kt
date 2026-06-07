package com.luminine.app.data.session

import kotlinx.serialization.Serializable

// One logged-in user's session. @Serializable so the DataStore impl can JSON-encode it into a
// single Preferences key. userId is generated at login time by the caller (e.g. "user-"+kakaoId).
@Serializable
data class Session(
    val userId: String,
    val kakaoId: String,
    val displayName: String,
    val onboardingComplete: Boolean = false,
)
