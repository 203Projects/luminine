package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveyStepTest {
    @Test fun flowOrderStartsAtS0AndEndsAtReward() {
        assertEquals(SurveyStep.S0, surveyFlowOrder.first())
        assertEquals(SurveyStep.Reward, surveyFlowOrder.last())
    }

    @Test fun noticeSitsBetweenS1AndS2() {
        assertEquals(SurveyStep.Notice, nextStep(SurveyStep.S1))
        assertEquals(SurveyStep.S2, nextStep(SurveyStep.Notice))
    }

    @Test fun requiredStepsAreNotSkippable() {
        listOf(SurveyStep.S0, SurveyStep.S1, SurveyStep.S6).forEach {
            assertTrue(!it.skippable, "$it must be required")
        }
    }

    @Test fun skippableStepsMatchSpec() {
        val skippable = surveyFlowOrder.filter { it.skippable }.toSet()
        assertEquals(
            setOf(SurveyStep.S2, SurveyStep.S3, SurveyStep.S4, SurveyStep.S5, SurveyStep.S7),
            skippable,
        )
    }

    @Test fun countedStepsAreTheEightContentSections() {
        assertEquals(8, countedSteps.size)
        assertTrue(countedSteps.none { it.isNotice || it.isReward })
    }

    @Test fun previousAtStartAndNextAtEndAreNull() {
        assertNull(previousStep(SurveyStep.S0))
        assertNull(nextStep(SurveyStep.Reward))
    }

    @Test fun progressIsMonotonicAcrossTheFlow() {
        var last = -1f
        // Notice intentionally repeats S1's fraction (it precedes S2), so allow >= for it.
        surveyFlowOrder.forEach { step ->
            val f = progressFraction(step)
            assertTrue(f in 0f..1f, "$step fraction out of range: $f")
            assertTrue(f >= last, "$step regressed: $f < $last")
            last = f
        }
        assertEquals(1f, progressFraction(SurveyStep.Reward))
    }

    @Test fun firstCountedStepHasNonZeroProgress() {
        assertEquals(1f / 8f, progressFraction(SurveyStep.S0))
    }

    @Test fun stepLabelShowsNOfEightForContentAndNullForNoticeReward() {
        assertEquals("단계 1/8", stepLabel(SurveyStep.S0))
        assertEquals("단계 8/8", stepLabel(SurveyStep.S7))
        assertNull(stepLabel(SurveyStep.Notice))
        assertNull(stepLabel(SurveyStep.Reward))
    }

    @Test fun everyContentStepMapsToAPersistedSection() {
        countedSteps.forEach { step ->
            assertTrue(step.section != null, "$step should map to a SurveySection")
        }
        // and the mapping covers S0..S7 exactly once
        assertEquals(SurveySection.entries.toSet(), countedSteps.mapNotNull { it.section }.toSet())
    }

    @Test fun rankedGoalsSortsByRankAscending() {
        val ranks = mapOf(
            PriorityGoal.SleepQuality to 2,
            PriorityGoal.SkinAging to 1,
            PriorityGoal.MuscleMaintain to 3,
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
