package com.luminine.app.onboarding

import com.luminine.app.data.decodeSurvey
import com.luminine.app.data.encodeToJson
import com.luminine.app.model.AlcoholFrequency
import com.luminine.app.model.AllergenComponent
import com.luminine.app.model.BedtimeRange
import com.luminine.app.model.BloodPressureStatus
import com.luminine.app.model.BloodSugarStatus
import com.luminine.app.model.BodyShapeSymptom
import com.luminine.app.model.CaffeineIntake
import com.luminine.app.model.CardioMetabolicCondition
import com.luminine.app.model.CognitiveSymptom
import com.luminine.app.model.ConsultingInterest
import com.luminine.app.model.DietRestriction
import com.luminine.app.model.DigestiveCondition
import com.luminine.app.model.DigestiveSymptom
import com.luminine.app.model.EnergySymptom
import com.luminine.app.model.ExerciseDuration
import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.ExerciseGoal
import com.luminine.app.model.ExerciseIntensity
import com.luminine.app.model.ExerciseType
import com.luminine.app.model.Gender
import com.luminine.app.model.HearingStatus
import com.luminine.app.model.HormonalSymptom
import com.luminine.app.model.HormoneCondition
import com.luminine.app.model.ImmuneAllergyCondition
import com.luminine.app.model.JobType
import com.luminine.app.model.JointPainSymptom
import com.luminine.app.model.MealCount
import com.luminine.app.model.MealRegularity
import com.luminine.app.model.MonthlyBudget
import com.luminine.app.model.MusculoskeletalCondition
import com.luminine.app.model.NeuroPsychCondition
import com.luminine.app.model.OtherCondition
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.model.RelaxationActivity
import com.luminine.app.model.SkinSymptom
import com.luminine.app.model.SleepAid
import com.luminine.app.model.SleepDuration
import com.luminine.app.model.SleepSymptom
import com.luminine.app.model.SmokingStatus
import com.luminine.app.model.StapleDietType
import com.luminine.app.model.StressSource
import com.luminine.app.model.Supplement
import com.luminine.app.model.SurveySection
import com.luminine.app.model.VisionStatus
import com.luminine.app.model.WalkingTime
import com.luminine.app.model.WaterIntake
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD red: drives the FULL SurveyDraft -> SurveyResponse mapping (every v1.0-spec field).
 *
 * Testable seam: SurveyDraft is plain Compose state and can be instantiated + mutated outside a
 * composition (global snapshot) on both :composeApp:allTests targets (Android host JVM +
 * iosSimulatorArm64). Tests construct SurveyDraft(), set fields, call toResponse(), and assert the
 * built SurveyResponse — no separate pure mapping function is extracted.
 *
 * These tests are EXPECTED to fail (compile-red) until the draft collects every model field.
 */
class SurveyDraftMappingTest {

    // Fills the required sections (S0/S1/S6) so toResponse() is well-formed; skippable sections
    // left untouched unless a test fills them.
    private fun SurveyDraft.fillRequiredSections() {
        name = "테스터"
        birthYear = 1985
        gender = Gender.Female
        region = Region.Seoul
        heightCm = "165"
        weightKg = "58"
        toggleGoal(PriorityGoal.SkinAging)
    }

    // ---- S0 기본 인적사항 ----

    @Test fun draftMapsBirthMonthAndDayIntoBasicInfo() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            birthMonth = 6
            birthDay = 2
        }
        val basic = draft.toResponse().basicInfo
        assertEquals("테스터", basic.name)
        assertEquals(1985, basic.birthYear)
        assertEquals(6, basic.birthMonth)
        assertEquals(2, basic.birthDay)
        assertEquals(Gender.Female, basic.gender)
        assertEquals(Region.Seoul, basic.region)
    }

    // ---- S1 신체 기본정보 ----

    @Test fun draftMapsAllBodyInfoCategoricalsAndWaist() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            waistCm = "82"
            bloodPressure = BloodPressureStatus.High
            bloodSugar = BloodSugarStatus.Diabetes
            vision = VisionStatus.GlassesOrLens
            hearing = HearingStatus.Impaired
        }
        val body = draft.toResponse().bodyInfo
        assertEquals(82.0, body.waistCm)
        assertEquals(BloodPressureStatus.High, body.bloodPressure)
        assertEquals(BloodSugarStatus.Diabetes, body.bloodSugar)
        assertEquals(VisionStatus.GlassesOrLens, body.vision)
        assertEquals(HearingStatus.Impaired, body.hearing)
        // sanity: existing required numerics still map.
        assertEquals(165.0, body.heightCm)
        assertEquals(58.0, body.weightKg)
    }

    @Test fun waistUnknownMapsWaistToNull() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            waistCm = "82"
            waistUnknown = true
        }
        assertNull(draft.toResponse().bodyInfo.waistCm)
    }

    // ---- S2 질환·병력 (all 7 groups + free text) ----

    @Test fun draftMapsAllSevenConditionGroups() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            cardioMetabolic.add(CardioMetabolicCondition.Hypertension)
            digestiveConditions.add(DigestiveCondition.Ibs)
            musculoskeletal.add(MusculoskeletalCondition.LumbarDisc)
            hormoneConditions.add(HormoneCondition.Hypothyroidism)
            neuroPsych.add(NeuroPsychCondition.Insomnia)
            immuneAllergy.add(ImmuneAllergyCondition.AtopicDermatitis)
            otherConditions.add(OtherCondition.KidneyDisease)
        }
        val c = assertNotNull(draft.toResponse().conditions)
        assertEquals(setOf(CardioMetabolicCondition.Hypertension), c.cardioMetabolic)
        assertEquals(setOf(DigestiveCondition.Ibs), c.digestive)
        assertEquals(setOf(MusculoskeletalCondition.LumbarDisc), c.musculoskeletal)
        assertEquals(setOf(HormoneCondition.Hypothyroidism), c.hormone)
        assertEquals(setOf(NeuroPsychCondition.Insomnia), c.neuroPsych)
        assertEquals(setOf(ImmuneAllergyCondition.AtopicDermatitis), c.immuneAllergy)
        assertEquals(setOf(OtherCondition.KidneyDisease), c.other)
    }

    @Test fun conditionFreeTextOnlyFlowsWhenProvided() {
        // 식품/환경 알레르기 free-text only flows when its controlling chip is selected (gated in
        // toResponse() so de-selected text can't leak). otherConditionText is always-on (no gate).
        val withText = SurveyDraft().apply {
            fillRequiredSections()
            immuneAllergy.add(ImmuneAllergyCondition.FoodAllergy)
            immuneAllergy.add(ImmuneAllergyCondition.EnvironmentalAllergy)
            foodAllergyText = "  복숭아  "
            environmentalAllergyText = "꽃가루"
            conditionsCustom = "희귀질환"
        }
        val c = assertNotNull(withText.toResponse().conditions)
        assertEquals("복숭아", c.foodAllergyText)
        assertEquals("꽃가루", c.environmentalAllergyText)
        assertEquals("희귀질환", c.otherConditionText)

        // Blank free-text -> null (even with the chips selected).
        val blank = SurveyDraft().apply {
            fillRequiredSections()
            immuneAllergy.add(ImmuneAllergyCondition.FoodAllergy)
            immuneAllergy.add(ImmuneAllergyCondition.EnvironmentalAllergy)
            foodAllergyText = "   "
            environmentalAllergyText = ""
            conditionsCustom = ""
        }
        val cb = assertNotNull(blank.toResponse().conditions)
        assertNull(cb.foodAllergyText)
        assertNull(cb.environmentalAllergyText)
        assertNull(cb.otherConditionText)
    }

    // ---- S3 걱정되는 건강 문제 (all 8 symptom groups) ----

    @Test fun draftMapsAllEightSymptomGroups() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            energySymptoms.add(EnergySymptom.ChronicFatigue)
            bodyShapeSymptoms.add(BodyShapeSymptom.AbdominalFat)
            skinSymptoms.add(SkinSymptom.Wrinkles)
            digestiveSymptoms.add(DigestiveSymptom.Constipation)
            sleepSymptoms.add(SleepSymptom.FrequentWaking)
            cognitiveSymptoms.add(CognitiveSymptom.MemoryDecline)
            hormonalSymptoms.add(HormonalSymptom.LowLibido)
            jointPainSymptoms.add(JointPainSymptom.KneePain)
        }
        val s = assertNotNull(draft.toResponse().symptoms)
        assertEquals(setOf(EnergySymptom.ChronicFatigue), s.energy)
        assertEquals(setOf(BodyShapeSymptom.AbdominalFat), s.bodyShape)
        assertEquals(setOf(SkinSymptom.Wrinkles), s.skin)
        assertEquals(setOf(DigestiveSymptom.Constipation), s.digestive)
        assertEquals(setOf(SleepSymptom.FrequentWaking), s.sleep)
        assertEquals(setOf(CognitiveSymptom.MemoryDecline), s.cognitive)
        assertEquals(setOf(HormonalSymptom.LowLibido), s.hormonal)
        assertEquals(setOf(JointPainSymptom.KneePain), s.jointPain)
    }

    // ---- S4 생활습관 (full ~20 fields) ----

    @Test fun draftMapsFullLifestyleMealBlock() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            mealCount = MealCount.Three
            mealRegularity = MealRegularity.Irregular
            stapleDietType = StapleDietType.Korean
            dietRestrictions.add(DietRestriction.GlutenFree)
            dietRestrictions.add(DietRestriction.None)
            waterIntake = WaterIntake.OneToOneHalf
            alcoholFrequency = AlcoholFrequency.WeeklyOneTwo
            smokingStatus = SmokingStatus.NonSmoker
            caffeineIntake = CaffeineIntake.TwoThree
        }
        val l = assertNotNull(draft.toResponse().lifestyle)
        assertEquals(MealCount.Three, l.mealCount)
        assertEquals(MealRegularity.Irregular, l.mealRegularity)
        assertEquals(StapleDietType.Korean, l.stapleDietType)
        assertEquals(setOf(DietRestriction.GlutenFree, DietRestriction.None), l.dietRestrictions)
        assertEquals(WaterIntake.OneToOneHalf, l.waterIntake)
        assertEquals(AlcoholFrequency.WeeklyOneTwo, l.alcoholFrequency)
        assertEquals(SmokingStatus.NonSmoker, l.smokingStatus)
        assertEquals(CaffeineIntake.TwoThree, l.caffeineIntake)
    }

    @Test fun draftMapsFullLifestyleExerciseBlock() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            exerciseFrequency = ExerciseFrequency.WeeklyThreeFour
            exerciseTypes.add(ExerciseType.Strength)
            exerciseTypes.add(ExerciseType.Swimming)
            exerciseIntensity = ExerciseIntensity.Moderate
            exerciseDuration = ExerciseDuration.ThirtyToSixty
            exerciseGoals.add(ExerciseGoal.MuscleGain)
        }
        val l = assertNotNull(draft.toResponse().lifestyle)
        assertEquals(ExerciseFrequency.WeeklyThreeFour, l.exerciseFrequency)
        assertEquals(setOf(ExerciseType.Strength, ExerciseType.Swimming), l.exerciseTypes)
        assertEquals(ExerciseIntensity.Moderate, l.exerciseIntensity)
        assertEquals(ExerciseDuration.ThirtyToSixty, l.exerciseDuration)
        assertEquals(setOf(ExerciseGoal.MuscleGain), l.exerciseGoals)
    }

    @Test fun draftMapsFullLifestyleSleepAndStressBlock() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            sleepDuration = SleepDuration.SevenToEight
            bedtime = BedtimeRange.TwentyTwoToMidnight
            sleepQuality = 4
            sleepAid = SleepAid.Other
            sleepAidOtherText = "  테아닌  "
            stressLevel = 3
            stressSources.add(StressSource.Work)
            relaxationActivities.add(RelaxationActivity.Meditation)
        }
        val l = assertNotNull(draft.toResponse().lifestyle)
        assertEquals(SleepDuration.SevenToEight, l.sleepDuration)
        assertEquals(BedtimeRange.TwentyTwoToMidnight, l.bedtime)
        assertEquals(4, l.sleepQuality)
        assertEquals(SleepAid.Other, l.sleepAid)
        assertEquals("테아닌", l.sleepAidOtherText)
        assertEquals(3, l.stressLevel)
        assertEquals(setOf(StressSource.Work), l.stressSources)
        assertEquals(setOf(RelaxationActivity.Meditation), l.relaxationActivities)
    }

    @Test fun sleepAidOtherTextBlankMapsToNull() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            sleepAid = SleepAid.None
            sleepAidOtherText = "   "
        }
        assertNull(assertNotNull(draft.toResponse().lifestyle).sleepAidOtherText)
    }

    // ---- S5 복용 중 (영양제/알레르기 기타 직접 입력) ----

    @Test fun draftMapsSupplementAndAllergenOtherText() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            supplements.add(Supplement.Omega3)
            supplementOtherText = "  홍삼  "
            takingPrescription = true
            prescriptionNote = "혈압약"
            allergens.add(AllergenComponent.Nuts)
            allergenOtherText = "메밀"
        }
        val sup = assertNotNull(draft.toResponse().supplements)
        assertEquals(setOf(Supplement.Omega3), sup.supplements)
        assertEquals("홍삼", sup.supplementOtherText)
        assertEquals(true, sup.takingPrescription)
        assertEquals("혈압약", sup.prescriptionText)
        assertEquals(setOf(AllergenComponent.Nuts), sup.allergens)
        assertEquals("메밀", sup.allergenOtherText)
    }

    @Test fun supplementAndAllergenOtherTextBlankMapsToNull() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            supplementOtherText = ""
            allergenOtherText = "   "
        }
        val sup = assertNotNull(draft.toResponse().supplements)
        assertNull(sup.supplementOtherText)
        assertNull(sup.allergenOtherText)
    }

    // ---- S7 라이프스타일 & 예산 (regression: still maps) ----

    @Test fun draftMapsBudgetLifestyle() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            jobType = JobType.OfficeSitting
            walkingTime = WalkingTime.ThirtyToOneHour
            monthlyBudget = MonthlyBudget.From100kTo300k
            consultingInterest = ConsultingInterest.Interested
        }
        val b = assertNotNull(draft.toResponse().budgetLifestyle)
        assertEquals(JobType.OfficeSitting, b.jobType)
        assertEquals(WalkingTime.ThirtyToOneHour, b.walkingTime)
        assertEquals(MonthlyBudget.From100kTo300k, b.monthlyBudget)
        assertEquals(ConsultingInterest.Interested, b.consultingInterest)
    }

    // ---- skip / complete semantics (must survive the expansion) ----

    @Test fun skippedSectionStillNullsOutEvenWithDataEntered() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            // Populate S4 lifestyle data...
            mealCount = MealCount.Three
            exerciseFrequency = ExerciseFrequency.WeeklyThreeFour
            sleepQuality = 4
            // ...then explicitly skip it.
            markSkipped(SurveySection.S4)
        }
        val r = draft.toResponse()
        assertNull(r.lifestyle)
        assertTrue(SurveySection.S4 in r.skippedSections)
        assertTrue(SurveySection.S4 !in r.completedSections)
    }

    @Test fun filledSkippableSectionIsCompletedNotSkipped() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            cardioMetabolic.add(CardioMetabolicCondition.Hypertension)
        }
        val r = draft.toResponse()
        assertNotNull(r.conditions)
        assertTrue(SurveySection.S2 in r.completedSections)
        assertTrue(SurveySection.S2 !in r.skippedSections)
    }

    @Test fun emptyUntouchedSkippableSectionsAreEmptyNotNullWhenNotSkipped() {
        // No S3 selections, S3 not skipped -> symptoms != null with all 8 Sets empty.
        val draft = SurveyDraft().apply { fillRequiredSections() }
        val s = assertNotNull(draft.toResponse().symptoms)
        assertTrue(s.energy.isEmpty())
        assertTrue(s.bodyShape.isEmpty())
        assertTrue(s.skin.isEmpty())
        assertTrue(s.digestive.isEmpty())
        assertTrue(s.sleep.isEmpty())
        assertTrue(s.cognitive.isEmpty())
        assertTrue(s.hormonal.isEmpty())
        assertTrue(s.jointPain.isEmpty())
    }

    // ---- conditional free-text hygiene: a value typed then de-selected must not leak ----

    @Test fun foodAllergyTextDropsWhenChipDeselected() {
        // User selects FoodAllergy, types text, then removes the chip (the UI field disappears).
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            immuneAllergy.add(ImmuneAllergyCondition.FoodAllergy)
            foodAllergyText = "복숭아"
            immuneAllergy.remove(ImmuneAllergyCondition.FoodAllergy) // de-select
        }
        assertNull(assertNotNull(draft.toResponse().conditions).foodAllergyText)
    }

    @Test fun environmentalAllergyTextDropsWhenChipDeselected() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            immuneAllergy.add(ImmuneAllergyCondition.EnvironmentalAllergy)
            environmentalAllergyText = "꽃가루"
            immuneAllergy.remove(ImmuneAllergyCondition.EnvironmentalAllergy)
        }
        assertNull(assertNotNull(draft.toResponse().conditions).environmentalAllergyText)
    }

    @Test fun sleepAidOtherTextDropsWhenSleepAidSwitchedAwayFromOther() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            sleepAid = SleepAid.Other
            sleepAidOtherText = "테아닌"
            sleepAid = SleepAid.Melatonin // switch away from Other
        }
        assertNull(assertNotNull(draft.toResponse().lifestyle).sleepAidOtherText)
    }

    @Test fun prescriptionTextDropsWhenNotTakingPrescription() {
        val draft = SurveyDraft().apply {
            fillRequiredSections()
            takingPrescription = true
            prescriptionNote = "혈압약"
            takingPrescription = false // user switches to 없음
        }
        assertNull(assertNotNull(draft.toResponse().supplements).prescriptionText)
    }

    // ---- the big round-trip: every newly-collected field is @Serializable-safe ----

    @Test fun fullyPopulatedDraftRoundTripsThroughJson() {
        val draft = SurveyDraft().apply {
            // S0
            name = "테스터"
            birthYear = 1985
            birthMonth = 6
            birthDay = 2
            gender = Gender.Female
            region = Region.Seoul
            // S1
            heightCm = "165"
            weightKg = "58"
            bodyFatPct = "22"
            muscleKg = "27"
            waistCm = "82"
            bloodPressure = BloodPressureStatus.High
            bloodSugar = BloodSugarStatus.Normal
            vision = VisionStatus.GlassesOrLens
            hearing = HearingStatus.Normal
            // S2
            cardioMetabolic.add(CardioMetabolicCondition.Hypertension)
            digestiveConditions.add(DigestiveCondition.Ibs)
            musculoskeletal.add(MusculoskeletalCondition.LumbarDisc)
            hormoneConditions.add(HormoneCondition.Hypothyroidism)
            neuroPsych.add(NeuroPsychCondition.Insomnia)
            immuneAllergy.add(ImmuneAllergyCondition.FoodAllergy)
            immuneAllergy.add(ImmuneAllergyCondition.EnvironmentalAllergy)
            otherConditions.add(OtherCondition.KidneyDisease)
            foodAllergyText = "복숭아"
            environmentalAllergyText = "꽃가루"
            conditionsCustom = "희귀질환"
            // S3
            energySymptoms.add(EnergySymptom.ChronicFatigue)
            bodyShapeSymptoms.add(BodyShapeSymptom.AbdominalFat)
            skinSymptoms.add(SkinSymptom.Wrinkles)
            digestiveSymptoms.add(DigestiveSymptom.Constipation)
            sleepSymptoms.add(SleepSymptom.FrequentWaking)
            cognitiveSymptoms.add(CognitiveSymptom.MemoryDecline)
            hormonalSymptoms.add(HormonalSymptom.LowLibido)
            jointPainSymptoms.add(JointPainSymptom.KneePain)
            // S4
            mealCount = MealCount.Three
            mealRegularity = MealRegularity.Regular
            stapleDietType = StapleDietType.Korean
            dietRestrictions.add(DietRestriction.GlutenFree)
            waterIntake = WaterIntake.OneToOneHalf
            alcoholFrequency = AlcoholFrequency.MonthlyOneTwo
            smokingStatus = SmokingStatus.NonSmoker
            caffeineIntake = CaffeineIntake.OneCup
            exerciseFrequency = ExerciseFrequency.WeeklyThreeFour
            exerciseTypes.add(ExerciseType.Strength)
            exerciseIntensity = ExerciseIntensity.Moderate
            exerciseDuration = ExerciseDuration.ThirtyToSixty
            exerciseGoals.add(ExerciseGoal.MuscleGain)
            sleepDuration = SleepDuration.SevenToEight
            bedtime = BedtimeRange.TwentyTwoToMidnight
            sleepQuality = 4
            sleepAid = SleepAid.Other
            sleepAidOtherText = "테아닌"
            stressLevel = 3
            stressSources.add(StressSource.Work)
            relaxationActivities.add(RelaxationActivity.Meditation)
            // S5
            supplements.add(Supplement.Omega3)
            supplementOtherText = "홍삼"
            takingPrescription = true
            prescriptionNote = "혈압약"
            allergens.add(AllergenComponent.Nuts)
            allergenOtherText = "메밀"
            // S6
            toggleGoal(PriorityGoal.SkinAging)
            toggleGoal(PriorityGoal.SleepQuality)
            // S7
            jobType = JobType.OfficeSitting
            walkingTime = WalkingTime.ThirtyToOneHour
            monthlyBudget = MonthlyBudget.From100kTo300k
            consultingInterest = ConsultingInterest.Interested
        }
        val response = draft.toResponse()
        assertEquals(response, decodeSurvey(response.encodeToJson()))
    }
}
