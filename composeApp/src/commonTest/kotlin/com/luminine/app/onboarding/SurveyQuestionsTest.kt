package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

class SurveyNavTest {
    private val first = surveyQuestions.first()
    private val last = surveyQuestions.last()

    @Test fun prevOfFirstIsNullAndNextOfLastIsNull() {
        assertNull(prevQuestion(first))
        assertNull(nextQuestion(last))
    }

    @Test fun nextAndPrevAreInverse() {
        val q = surveyQuestions[3]
        assertEquals(q, prevQuestion(nextQuestion(q)!!))
    }

    @Test fun progressIsMonotonicAndBounded() {
        var lastF = -1f
        surveyQuestions.forEach { q ->
            val f = progressFraction(q)
            assertTrue(f in 0f..1f, "${q.id} out of range: $f")
            assertTrue(f >= lastF, "${q.id} regressed: $f < $lastF")
            lastF = f
        }
        assertEquals(1f, progressFraction(last))
    }

    @Test fun segmentInfoCoversEightSections() {
        // segmentInfo returns (sectionIndex 0..7, fractionWithinSection 0..1).
        val (idx0, _) = segmentInfo(first)
        assertEquals(0, idx0)
        val (idxLast, fracLast) = segmentInfo(last)
        assertEquals(7, idxLast) // reward is in S7
        assertEquals(1f, fracLast)
    }

    @Test fun isAnsweredFalseForUntouchedRequiredSingleChoice() {
        // Identity (S0) is unanswered on a fresh draft (s0Valid == false).
        val draft = SurveyDraft()
        assertEquals(false, isAnswered(first, draft))
    }

    @Test fun infoScreensAreAlwaysAnswered() {
        val info = surveyQuestions.first { it is SurveyQuestion.Info }
        assertTrue(isAnswered(info, SurveyDraft()))
    }

    @Test fun multiChoiceIsAlwaysAnsweredEvenWhenEmpty() {
        val multi = surveyQuestions.first { it is SurveyQuestion.MultiChoice<*> }
        assertTrue(isAnswered(multi, SurveyDraft()))
    }

    @Test fun rankedIsUnansweredUntilAGoalIsPicked() {
        val ranked = surveyQuestions.first { it is SurveyQuestion.Ranked }
        val draft = SurveyDraft()
        assertEquals(false, isAnswered(ranked, draft))
        draft.toggleGoal(PriorityGoal.SkinAging)
        assertTrue(isAnswered(ranked, draft))
    }
}
