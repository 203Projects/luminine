package com.luminine.app.data

import com.luminine.app.model.AllergenComponent
import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BloodPressureStatus
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.CardioMetabolicCondition
import com.luminine.app.model.ConditionsSection
import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.Gender
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.LifestyleSection
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.model.SkinSymptom
import com.luminine.app.model.Supplement
import com.luminine.app.model.SupplementsSection
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.model.SymptomsSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SurveyJsonTest {
    @Test fun emptySurveyRoundTrips() {
        val original = SurveyResponse()
        assertEquals(original, decodeSurvey(original.encodeToJson()))
    }

    @Test fun requiredOnlySurveyRoundTrips() {
        val original = SurveyResponse(
            basicInfo = BasicInfoSection(name = "김민지", birthYear = 1990, birthMonth = 6, birthDay = 2, gender = Gender.Female, region = Region.Seoul),
            bodyInfo = BodyInfoSection(heightCm = 165.0, weightKg = 58.0, bloodPressure = BloodPressureStatus.Normal),
            goals = GoalsSection(orderedGoals = listOf(PriorityGoal.SkinAging, PriorityGoal.SleepQuality)),
            completedSections = setOf(SurveySection.S0, SurveySection.S1, SurveySection.S6),
        )
        assertEquals(original, decodeSurvey(original.encodeToJson()))
    }

    @Test fun fullSurveyWithAllSectionsRoundTrips() {
        val original = SurveyResponse(
            basicInfo = BasicInfoSection(name = "테스터", birthYear = 1985, gender = Gender.Other, region = Region.Jeju),
            bodyInfo = BodyInfoSection(heightCm = 170.0, weightKg = 70.0, bodyFatPct = null, muscleMassKg = 30.0),
            conditions = ConditionsSection(
                cardioMetabolic = setOf(CardioMetabolicCondition.Hypertension),
                foodAllergyText = "복숭아",
            ),
            symptoms = SymptomsSection(skin = setOf(SkinSymptom.Wrinkles, SkinSymptom.ElasticityLoss)),
            lifestyle = LifestyleSection(exerciseFrequency = ExerciseFrequency.WeeklyThreeFour, sleepQuality = 3, stressLevel = 4),
            supplements = SupplementsSection(
                supplements = setOf(Supplement.VitaminD, Supplement.Omega3),
                takingPrescription = true,
                prescriptionText = "혈압약",
                allergens = setOf(AllergenComponent.Shellfish),
            ),
            goals = GoalsSection(orderedGoals = PriorityGoal.entries.toList()),
            budgetLifestyle = null,
            completedSections = SurveySection.entries.toSet(),
            schemaVersion = 1,
        )
        assertEquals(original, decodeSurvey(original.encodeToJson()))
    }

    @Test fun skippedSectionsRoundTrip() {
        val original = SurveyResponse(
            basicInfo = BasicInfoSection(name = "건너뛴", birthYear = 1995, gender = Gender.Female, region = Region.Gyeonggi),
            bodyInfo = BodyInfoSection(heightCm = 160.0, weightKg = 52.0),
            goals = GoalsSection(orderedGoals = listOf(PriorityGoal.SkinAging)),
            skippedSections = setOf(SurveySection.S2, SurveySection.S3, SurveySection.S4, SurveySection.S5, SurveySection.S7),
        )
        val decoded = decodeSurvey(original.encodeToJson())
        assertEquals(original, decoded)
        assertEquals(5, decoded.skippedSections.size)
    }

    @Test fun skippedSectionsRemainNullAfterRoundTrip() {
        val original = SurveyResponse(
            basicInfo = BasicInfoSection(name = "a", birthYear = 2000, gender = Gender.Male, region = Region.Busan),
            bodyInfo = BodyInfoSection(heightCm = 180.0, weightKg = 75.0),
            goals = GoalsSection(orderedGoals = listOf(PriorityGoal.MuscleMaintain)),
        )
        val decoded = decodeSurvey(original.encodeToJson())
        assertNull(decoded.conditions)
        assertNull(decoded.symptoms)
        assertNull(decoded.lifestyle)
        assertNull(decoded.supplements)
        assertNull(decoded.budgetLifestyle)
    }

    @Test fun unknownKeysAreIgnoredForForwardCompat() {
        // Simulate a future field added by a newer schema version.
        val json = "{\"schemaVersion\":2,\"futureField\":\"x\",\"goals\":{\"orderedGoals\":[\"SkinAging\"]}}"
        val decoded = decodeSurvey(json)
        assertEquals(listOf(PriorityGoal.SkinAging), decoded.goals.orderedGoals)
        assertEquals(2, decoded.schemaVersion)
    }
}
