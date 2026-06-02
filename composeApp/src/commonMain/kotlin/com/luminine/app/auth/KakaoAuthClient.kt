package com.luminine.app.auth

// Kakao account returned by a successful login. Mirrors the fields the real Kakao SDK exposes,
// kept minimal for what the app needs (id + nickname + optional avatar).
data class KakaoAccount(
    val kakaoId: String,
    val nickname: String,
    val profileImageUrl: String? = null,
)

sealed interface AuthResult {
    data class Success(val account: KakaoAccount) : AuthResult
    data object Cancelled : AuthResult
    data class Error(val message: String) : AuthResult
}

// Auth seam. The real per-platform Kakao SDK clients will later implement this same interface in
// androidMain/iosMain; this slice ships only the stub. All in commonMain — zero platform deps.
interface KakaoAuthClient {
    suspend fun login(): AuthResult
}
