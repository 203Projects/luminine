package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

// Regressions for the adversarial-review findings (2026-06-09).
class SurveyReviewFixTest {
    private val reward = surveyQuestions.last()

    // BLOCKER: skipping any S7 question must land on the reward, not complete prematurely.
    @Test fun skipFromAnyS7QuestionLandsOnReward() {
        val s7Skippable = surveyQuestions.filter {
            it.section == com.luminine.app.model.SurveySection.S7 && it.skippable
        }
        assertTrue(s7Skippable.isNotEmpty(), "expected skippable S7 questions")
        s7Skippable.forEach { q ->
            assertEquals(reward, skipTarget(q), "skip from ${q.id} should land on the reward")
        }
    }

    // skipTarget for a mid-flow skippable section lands on the NEXT section's first question.
    @Test fun skipFromS2LandsOnNextSectionNotReward() {
        val s2 = surveyQuestions.first { it.section == com.luminine.app.model.SurveySection.S2 && it.skippable }
        val target = skipTarget(s2)
        assertNotNull(target)
        assertTrue(target!!.section != com.luminine.app.model.SurveySection.S2)
        assertTrue(!(target is SurveyQuestion.Info && target.kind == InfoKind.Reward), "should not be the reward")
    }

    // Tone guardrail: checkpoints for the medical/sensitive sections (S2/S3/S5) are calm (empty prompt).
    @Test fun medicalSectionCheckpointsAreCalm() {
        listOf("s2.done", "s3.done", "s5.done").forEach { id ->
            val cp = surveyQuestions.first { it.id == id } as SurveyQuestion.Info
            assertEquals("", cp.prompt, "$id checkpoint should be calm (empty prompt)")
        }
    }

    // The 4 free-text fields the prior full-survey UI collected must be reachable in the gamified flow.
    @Test fun droppedFreeTextFieldsAreWiredViaExtraText() {
        fun extraOf(id: String): ExtraText? = when (val q = surveyQuestions.first { it.id == id }) {
            is SurveyQuestion.MultiChoice<*> -> q.extraText
            is SurveyQuestion.SingleChoice<*> -> q.extraText
            else -> null
        }
        listOf("s2.other", "s4.sleepaid", "s5.supps", "s5.allergen").forEach { id ->
            assertNotNull(extraOf(id), "$id must declare an extraText free-text field")
        }
        // sleepaid's note is gated on SleepAid.Other; the others are always-on.
        val sleep = extraOf("s4.sleepaid")!!
        val d0 = SurveyDraft()
        assertTrue(!sleep.showWhen(d0), "sleepaid extra hidden until Other selected")
        d0.sleepAid = com.luminine.app.model.SleepAid.Other
        assertTrue(sleep.showWhen(d0), "sleepaid extra shows when Other selected")
        assertTrue(extraOf("s5.supps")!!.showWhen(SurveyDraft()), "supplement extra is always-on")
    }
}
