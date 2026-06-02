package com.luminine.app.data.session

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemorySessionRepositoryTest {
    private val sample = Session(
        userId = "user-kakao-stub-1001",
        kakaoId = "kakao-stub-1001",
        displayName = "김민지",
        onboardingComplete = false,
    )

    @Test
    fun loadReturnsNullBeforeAnySave() = runTest {
        assertNull(InMemorySessionRepository().load())
    }

    @Test
    fun saveThenLoadRoundTripsTheSession() = runTest {
        val repo = InMemorySessionRepository()
        repo.save(sample)
        assertEquals(sample, repo.load())
    }

    @Test
    fun saveOverwritesPreviousSession() = runTest {
        val repo = InMemorySessionRepository()
        repo.save(sample)
        repo.save(sample.copy(onboardingComplete = true))
        assertEquals(true, repo.load()?.onboardingComplete)
    }

    @Test
    fun clearRemovesTheSession() = runTest {
        val repo = InMemorySessionRepository()
        repo.save(sample)
        repo.clear()
        assertNull(repo.load())
    }

    @Test
    fun initialSessionIsLoadable() = runTest {
        assertEquals(sample, InMemorySessionRepository(initial = sample).load())
    }
}
