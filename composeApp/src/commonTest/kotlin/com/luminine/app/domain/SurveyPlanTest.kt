package com.luminine.app.domain

import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.Gender
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// A fully-valid minimal required survey.
private fun validRequired(
    name: String = "김민지",
    goals: List<PriorityGoal> = listOf(PriorityGoal.SkinAging),
) = SurveyResponse(
    basicInfo = BasicInfoSection(name = name, birthYear = 1990, gender = Gender.Female, region = Region.Seoul),
    bodyInfo = BodyInfoSection(heightCm = 165.0, weightKg = 58.0),
    goals = GoalsSection(orderedGoals = goals),
)

class SurveyValidationTest {
    @Test fun canCompleteWhenRequiredSectionsFilled() {
        assertTrue(canCompleteOnboarding(validRequired()))
    }

    @Test fun cannotCompleteWhenNameBlank() {
        assertFalse(canCompleteOnboarding(validRequired(name = "  ")))
    }

    @Test fun cannotCompleteWhenNoGoals() {
        assertFalse(canCompleteOnboarding(validRequired(goals = emptyList())))
    }

    @Test fun cannotCompleteWhenHeightOrWeightMissing() {
        val s = validRequired().copy(bodyInfo = BodyInfoSection(heightCm = 165.0, weightKg = null))
        assertFalse(canCompleteOnboarding(s))
    }

    @Test fun missingRequiredSectionsReportsAllThreeWhenEmpty() {
        assertEquals(
            setOf(SurveySection.S0, SurveySection.S1, SurveySection.S6),
            missingRequiredSections(SurveyResponse()),
        )
    }

    @Test fun missingRequiredSectionsEmptyWhenComplete() {
        assertTrue(missingRequiredSections(validRequired()).isEmpty())
    }
}

// Goal-to-category mapping and routine seeding are exercised by SurveySeedingTest.kt
// (SeededCategoriesTest / SeededRoutinesTest), which tests the live seededCategories/seededRoutines
// path used by App.kt. This file covers onboarding-completion validation.
