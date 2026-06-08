package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurveyQuestionsTest {
    @Test fun registryIsNonEmptyAndStartsWithBasicInfo() {
        assertTrue(surveyQuestions.isNotEmpty())
        assertEquals(SurveySection.S0, surveyQuestions.first().section)
    }

    @Test fun registryEndsWithTheRewardInfoScreen() {
        val last = surveyQuestions.last()
        assertTrue(last is SurveyQuestion.Info && last.kind == InfoKind.Reward)
    }

    @Test fun everySectionIsRepresented() {
        val sections = surveyQuestions.map { it.section }.toSet()
        assertEquals(SurveySection.entries.toSet(), sections)
    }

    @Test fun onlySkippableSectionsHaveSkippableQuestions() {
        val skippableSections = setOf(
            SurveySection.S2, SurveySection.S3, SurveySection.S4, SurveySection.S5, SurveySection.S7,
        )
        surveyQuestions.filter { it.skippable }.forEach {
            assertTrue(it.section in skippableSections, "${it.id} skippable but section ${it.section} is required")
        }
    }

    @Test fun questionIdsAreUnique() {
        val ids = surveyQuestions.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate question id present")
    }

    @Test fun rankedGoalsSortsByRankAscending() {
        val ranks = mapOf(
            PriorityGoal.SleepQuality to 2, PriorityGoal.SkinAging to 1, PriorityGoal.MuscleMaintain to 3,
        )
        assertEquals(
            listOf(PriorityGoal.SkinAging, PriorityGoal.SleepQuality, PriorityGoal.MuscleMaintain),
            rankedGoals(ranks),
        )
    }

    @Test fun rankedGoalsEmptyWhenNoSelection() {
        assertTrue(rankedGoals(emptyMap()).isEmpty())
    }
}
