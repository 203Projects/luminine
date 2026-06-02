package com.luminine.app.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StubKakaoAuthClientTest {
    @Test
    fun loginReturnsSuccessWithStableDeterministicAccount() = runTest {
        val result = StubKakaoAuthClient().login()
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("kakao-stub-1001", success.account.kakaoId)
        assertEquals("김민지", success.account.nickname)
        assertEquals(null, success.account.profileImageUrl)
    }

    @Test
    fun loginIsRepeatableAcrossCalls() = runTest {
        val client = StubKakaoAuthClient()
        val a = assertIs<AuthResult.Success>(client.login())
        val b = assertIs<AuthResult.Success>(client.login())
        assertEquals(a.account, b.account)
    }

    @Test
    fun injectedAccountIsReturned() = runTest {
        val custom = KakaoAccount(kakaoId = "kakao-test-9", nickname = "테스터")
        val success = assertIs<AuthResult.Success>(StubKakaoAuthClient(custom).login())
        assertEquals(custom, success.account)
    }
}
