package com.luminine.app.data.survey

import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveyResponse
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemorySurveyRepositoryTest {
    private fun sampleResponse(): SurveyResponse = SurveyResponse(
        basicInfo = BasicInfoSection(name = "김민지", birthYear = 1990),
        bodyInfo = BodyInfoSection(heightCm = 165.0, weightKg = 58.0),
        goals = GoalsSection(orderedGoals = listOf(PriorityGoal.SkinAging)),
    )

    @Test
    fun loadReturnsNullBeforeAnySave() = runTest {
        assertNull(InMemorySurveyRepository().load())
    }

    @Test
    fun saveThenLoadRoundTripsTheSurvey() = runTest {
        val repo = InMemorySurveyRepository()
        val r = sampleResponse()
        repo.save(r)
        assertEquals(r, repo.load())
    }

    @Test
    fun clearRemovesTheSurvey() = runTest {
        val repo = InMemorySurveyRepository()
        repo.save(sampleResponse())
        repo.clear()
        assertNull(repo.load())
    }
}
