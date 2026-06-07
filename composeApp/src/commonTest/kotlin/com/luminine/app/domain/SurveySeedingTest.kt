package com.luminine.app.domain

import com.luminine.app.data.SampleData
import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.LifestyleSection
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.RoutineCategory
import com.luminine.app.model.SurveyResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun surveyWith(
    goals: List<PriorityGoal> = emptyList(),
    exercise: ExerciseFrequency? = null,
) = SurveyResponse(
    basicInfo = BasicInfoSection(name = "김민지"),
    bodyInfo = BodyInfoSection(heightCm = 165.0, weightKg = 58.0),
    goals = GoalsSection(orderedGoals = goals),
    lifestyle = exercise?.let { LifestyleSection(exerciseFrequency = it) },
)

class SeededCategoriesTest {
    @Test fun goalsMapToTwoCategoriesEachOrderedAndDeduped() {
        // SleepQuality -> [Sleep, Mind]; Cognitive -> [Mind, InnerCare] => Mind dedupes.
        val cats = seededCategories(surveyWith(goals = listOf(PriorityGoal.SleepQuality, PriorityGoal.Cognitive)))
        assertEquals(
            listOf(RoutineCategory.Sleep, RoutineCategory.Mind, RoutineCategory.InnerCare),
            cats,
        )
    }

    @Test fun noGoalsFallsBackToAllCategories() {
        assertEquals(RoutineCategory.entries.toSet(), seededCategories(surveyWith()).toSet())
    }

    @Test fun sedentaryNudgesExerciseIn() {
        // SkinAging -> [SkinCare, InnerCare]; Never exercise adds Exercise.
        val cats = seededCategories(surveyWith(goals = listOf(PriorityGoal.SkinAging), exercise = ExerciseFrequency.Never))
        assertTrue(RoutineCategory.Exercise in cats)
    }

    @Test fun activeUserDoesNotGetExtraExerciseNudge() {
        val cats = seededCategories(surveyWith(goals = listOf(PriorityGoal.SkinAging), exercise = ExerciseFrequency.AlmostDaily))
        assertFalse(RoutineCategory.Exercise in cats)
    }
}

class SeededRoutinesTest {
    @Test fun preservesIdsAndOrderOnlyFlippingIsActive() {
        val defaults = SampleData.defaultRoutines("user-1")
        val survey = surveyWith(goals = listOf(PriorityGoal.SkinAging)) // -> SkinCare, InnerCare
        val seeded = seededRoutines(survey, defaults)

        // Same routines (ids/order/count), only isActive changes.
        assertEquals(defaults.map { it.id }, seeded.map { it.id })
        assertEquals(defaults.map { it.order }, seeded.map { it.order })
        assertEquals(defaults.size, seeded.size)

        val active = seeded.filter { it.isActive }.map { it.category }.toSet()
        assertEquals(setOf(RoutineCategory.SkinCare, RoutineCategory.InnerCare), active)
    }

    @Test fun noGoalsKeepsAllRoutinesActive() {
        val defaults = SampleData.defaultRoutines("user-1")
        val seeded = seededRoutines(surveyWith(), defaults)
        assertTrue(seeded.all { it.isActive })
    }
}

class GreetingHelpersTest {
    @Test fun greetingUsesNameWhenPresent() {
        assertEquals("안녕하세요, 민지님", greetingFor("민지"))
    }

    @Test fun greetingFallsBackWhenBlank() {
        assertEquals("안녕하세요!", greetingFor("  "))
    }

    @Test fun avatarInitialTakesFirstChar() {
        assertEquals("김", avatarInitial("김민지"))
    }

    @Test fun avatarInitialFallsBackToL() {
        assertEquals("L", avatarInitial("   "))
    }
}
