package com.luminine.app.auth

/**
 * Deterministic fake Kakao client so the full auth -> survey -> home flow runs with no Kakao keys.
 * Always returns Success with a stable account, keeping tests and screenshots reproducible.
 * Swap in a real per-platform KakaoAuthClient implementation when SDK keys are wired.
 */
class StubKakaoAuthClient(
    private val account: KakaoAccount = DEFAULT_ACCOUNT,
) : KakaoAuthClient {
    override suspend fun login(): AuthResult = AuthResult.Success(account)

    companion object {
        val DEFAULT_ACCOUNT = KakaoAccount(
            kakaoId = "kakao-stub-1001",
            nickname = "김민지",
            profileImageUrl = null,
        )
    }
}
