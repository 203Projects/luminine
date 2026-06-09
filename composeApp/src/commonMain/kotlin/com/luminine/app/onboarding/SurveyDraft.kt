package com.luminine.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luminine.app.model.AlcoholFrequency
import com.luminine.app.model.AllergenComponent
import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BedtimeRange
import com.luminine.app.model.BloodPressureStatus
import com.luminine.app.model.BloodSugarStatus
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.BodyShapeSymptom
import com.luminine.app.model.BudgetLifestyleSection
import com.luminine.app.model.CaffeineIntake
import com.luminine.app.model.CardioMetabolicCondition
import com.luminine.app.model.CognitiveSymptom
import com.luminine.app.model.ConditionsSection
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
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.HearingStatus
import com.luminine.app.model.HormonalSymptom
import com.luminine.app.model.HormoneCondition
import com.luminine.app.model.ImmuneAllergyCondition
import com.luminine.app.model.JobType
import com.luminine.app.model.JointPainSymptom
import com.luminine.app.model.LifestyleSection
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
import com.luminine.app.model.SupplementsSection
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.model.SymptomsSection
import com.luminine.app.model.VisionStatus
import com.luminine.app.model.WalkingTime
import com.luminine.app.model.WaterIntake

// Mutable in-progress survey holder, hoisted by SurveyFlow so each section's state survives
// back/next navigation. Compose-aware (mutableStateOf). Converts to the canonical, persisted
// SurveyResponse via toResponse(). NOT unit-tested directly via Compose UI — but the pure mapping
// (toResponse(), validity gates) IS exercised off-composition (global snapshot) by
// SurveyDraftMappingTest, and the rank helper (rankedGoals) lives in SurveyQuestions.kt.
//
// This draft now collects the FULL v1.0 survey model (every field of SurveyModels.kt), not just an
// MVP subset: all S0 birthdate fields, all S1 categorical body statuses + 복부둘레, the 7 S2 condition
// groups + allergy/기타 free-text, all 8 S3 symptom groups, the full S4 생활습관 set, and the S5
// 영양제/알레르기 기타 직접 입력 fields.
class SurveyDraft {
    // --- S0 기본 인적사항 (required: birthYear; month/day optional) ---
    var name by mutableStateOf("")
    var birthYear by mutableStateOf<Int?>(null)
    var birthMonth by mutableStateOf<Int?>(null)
    var birthDay by mutableStateOf<Int?>(null)
    var gender by mutableStateOf<Gender?>(null)
    var region by mutableStateOf<Region?>(null)

    // --- S1 신체 기본정보 (required: height + weight; others 모름/null) ---
    var heightCm by mutableStateOf("")
    var weightKg by mutableStateOf("")
    var bodyFatPct by mutableStateOf("")
    var bodyFatUnknown by mutableStateOf(false)
    var muscleKg by mutableStateOf("")
    var muscleUnknown by mutableStateOf(false)
    var waistCm by mutableStateOf("")
    var waistUnknown by mutableStateOf(false)
    var bloodPressure by mutableStateOf<BloodPressureStatus?>(null)
    var bloodSugar by mutableStateOf<BloodSugarStatus?>(null)
    var vision by mutableStateOf<VisionStatus?>(null)
    var hearing by mutableStateOf<HearingStatus?>(null)

    // --- S2 질환·병력 (skippable) — 7 grouped multi-selects + free text ---
    val cardioMetabolic = mutableStateListOf<CardioMetabolicCondition>()
    val digestiveConditions = mutableStateListOf<DigestiveCondition>()
    val musculoskeletal = mutableStateListOf<MusculoskeletalCondition>()
    val hormoneConditions = mutableStateListOf<HormoneCondition>()
    val neuroPsych = mutableStateListOf<NeuroPsychCondition>()
    val immuneAllergy = mutableStateListOf<ImmuneAllergyCondition>()
    val otherConditions = mutableStateListOf<OtherCondition>()
    var foodAllergyText by mutableStateOf("")          // 식품 알레르기 직접 입력
    var environmentalAllergyText by mutableStateOf("") // 환경성 알레르기 직접 입력
    // 기타 직접 입력 — maps to ConditionsSection.otherConditionText (field name kept to avoid churn).
    var conditionsCustom by mutableStateOf("")

    // --- S3 체감 증상 (skippable) — all 8 symptom groups ---
    val energySymptoms = mutableStateListOf<EnergySymptom>()
    val bodyShapeSymptoms = mutableStateListOf<BodyShapeSymptom>()
    val skinSymptoms = mutableStateListOf<SkinSymptom>()
    val digestiveSymptoms = mutableStateListOf<DigestiveSymptom>()
    val sleepSymptoms = mutableStateListOf<SleepSymptom>()
    val cognitiveSymptoms = mutableStateListOf<CognitiveSymptom>()
    val hormonalSymptoms = mutableStateListOf<HormonalSymptom>()
    val jointPainSymptoms = mutableStateListOf<JointPainSymptom>()

    // --- S4 생활습관 (skippable) — exercise drives Home seeding ---
    // 식사 패턴
    var mealCount by mutableStateOf<MealCount?>(null)
    var mealRegularity by mutableStateOf<MealRegularity?>(null)
    var stapleDietType by mutableStateOf<StapleDietType?>(null)
    val dietRestrictions = mutableStateListOf<DietRestriction>()
    var waterIntake by mutableStateOf<WaterIntake?>(null)
    var alcoholFrequency by mutableStateOf<AlcoholFrequency?>(null)
    var smokingStatus by mutableStateOf<SmokingStatus?>(null)
    var caffeineIntake by mutableStateOf<CaffeineIntake?>(null)
    // 운동
    var exerciseFrequency by mutableStateOf<ExerciseFrequency?>(null)
    val exerciseTypes = mutableStateListOf<ExerciseType>()
    var exerciseIntensity by mutableStateOf<ExerciseIntensity?>(null)
    var exerciseDuration by mutableStateOf<ExerciseDuration?>(null)
    val exerciseGoals = mutableStateListOf<ExerciseGoal>()
    // 수면
    var sleepDuration by mutableStateOf<SleepDuration?>(null)
    var bedtime by mutableStateOf<BedtimeRange?>(null)
    var sleepQuality by mutableStateOf<Int?>(null) // 1..5 자가평가
    var sleepAid by mutableStateOf<SleepAid?>(null)
    var sleepAidOtherText by mutableStateOf("")
    // 스트레스 / 정신
    var stressLevel by mutableStateOf<Int?>(null) // 1..5
    val stressSources = mutableStateListOf<StressSource>()
    val relaxationActivities = mutableStateListOf<RelaxationActivity>()

    // --- S5 복용 중 (skippable) ---
    val supplements = mutableStateListOf<Supplement>()
    var supplementOtherText by mutableStateOf("") // 영양제 기타 직접 입력
    var takingPrescription by mutableStateOf<Boolean?>(null) // null=미입력, false=없음, true=있음
    var prescriptionNote by mutableStateOf("")
    val allergens = mutableStateListOf<AllergenComponent>()
    var allergenOtherText by mutableStateOf("") // 알레르기 성분 기타 직접 입력

    // --- S6 관심영역 우선순위 (required) — tapping assigns the next rank ---
    val goalRanks = mutableStateMapOf<PriorityGoal, Int>()

    fun toggleGoal(goal: PriorityGoal) {
        val existing = goalRanks[goal]
        if (existing != null) {
            goalRanks.remove(goal)
            // Keep ranks contiguous 1..n after removal.
            goalRanks.keys.toList().forEach { g ->
                val r = goalRanks[g]
                if (r != null && r > existing) goalRanks[g] = r - 1
            }
        } else {
            goalRanks[goal] = (goalRanks.values.maxOrNull() ?: 0) + 1
        }
    }

    // --- S7 라이프스타일 & 예산 (skippable) ---
    var jobType by mutableStateOf<JobType?>(null)
    var walkingTime by mutableStateOf<WalkingTime?>(null)
    var monthlyBudget by mutableStateOf<MonthlyBudget?>(null)
    var consultingInterest by mutableStateOf<ConsultingInterest?>(null)

    // Sections the user tapped "나중에 입력".
    val skipped = mutableStateListOf<SurveySection>()

    fun markSkipped(section: SurveySection) {
        if (section !in skipped) skipped.add(section)
    }

    // Validity gates for required-section "next" enablement. Only the spec-required fields gate the
    // next button — the newly-collected optional fields (birthMonth/day, waist, categoricals) never
    // do, so SurveyPlan.isBasicInfoComplete/isBodyInfoComplete and the progress contract are unaffected.
    val s0Valid: Boolean
        get() = name.isNotBlank() && birthYear != null && gender != null && region != null
    val s1Valid: Boolean
        get() = heightCm.toDoubleOrNull() != null && weightKg.toDoubleOrNull() != null
    val s6Valid: Boolean
        get() = goalRanks.isNotEmpty()

    fun toResponse(): SurveyResponse {
        val completed = buildSet {
            add(SurveySection.S0); add(SurveySection.S1); add(SurveySection.S6)
            if (SurveySection.S2 !in skipped) add(SurveySection.S2)
            if (SurveySection.S3 !in skipped) add(SurveySection.S3)
            if (SurveySection.S4 !in skipped) add(SurveySection.S4)
            if (SurveySection.S5 !in skipped) add(SurveySection.S5)
            if (SurveySection.S7 !in skipped) add(SurveySection.S7)
        }
        return SurveyResponse(
            basicInfo = BasicInfoSection(
                name = name.trim(),
                birthYear = birthYear,
                birthMonth = birthMonth,
                birthDay = birthDay,
                gender = gender,
                region = region,
            ),
            bodyInfo = BodyInfoSection(
                heightCm = heightCm.toDoubleOrNull(),
                weightKg = weightKg.toDoubleOrNull(),
                bodyFatPct = if (bodyFatUnknown) null else bodyFatPct.toDoubleOrNull(),
                muscleMassKg = if (muscleUnknown) null else muscleKg.toDoubleOrNull(),
                waistCm = if (waistUnknown) null else waistCm.toDoubleOrNull(),
                bloodPressure = bloodPressure,
                bloodSugar = bloodSugar,
                vision = vision,
                hearing = hearing,
            ),
            conditions = if (SurveySection.S2 in skipped) null else ConditionsSection(
                cardioMetabolic = cardioMetabolic.toSet(),
                digestive = digestiveConditions.toSet(),
                musculoskeletal = musculoskeletal.toSet(),
                hormone = hormoneConditions.toSet(),
                neuroPsych = neuroPsych.toSet(),
                immuneAllergy = immuneAllergy.toSet(),
                other = otherConditions.toSet(),
                // Conditional free-text is gated on its controlling chip so a value typed then
                // un-selected (the field disappears from the UI) never leaks onto the response.
                foodAllergyText = if (ImmuneAllergyCondition.FoodAllergy in immuneAllergy) foodAllergyText.trim().ifBlank { null } else null,
                environmentalAllergyText = if (ImmuneAllergyCondition.EnvironmentalAllergy in immuneAllergy) environmentalAllergyText.trim().ifBlank { null } else null,
                otherConditionText = conditionsCustom.trim().ifBlank { null },
            ),
            symptoms = if (SurveySection.S3 in skipped) null else SymptomsSection(
                energy = energySymptoms.toSet(),
                bodyShape = bodyShapeSymptoms.toSet(),
                skin = skinSymptoms.toSet(),
                digestive = digestiveSymptoms.toSet(),
                sleep = sleepSymptoms.toSet(),
                cognitive = cognitiveSymptoms.toSet(),
                hormonal = hormonalSymptoms.toSet(),
                jointPain = jointPainSymptoms.toSet(),
            ),
            lifestyle = if (SurveySection.S4 in skipped) null else LifestyleSection(
                // 식사 패턴
                mealCount = mealCount,
                mealRegularity = mealRegularity,
                stapleDietType = stapleDietType,
                dietRestrictions = dietRestrictions.toSet(),
                waterIntake = waterIntake,
                alcoholFrequency = alcoholFrequency,
                smokingStatus = smokingStatus,
                caffeineIntake = caffeineIntake,
                // 운동
                exerciseFrequency = exerciseFrequency,
                exerciseTypes = exerciseTypes.toSet(),
                exerciseIntensity = exerciseIntensity,
                exerciseDuration = exerciseDuration,
                exerciseGoals = exerciseGoals.toSet(),
                // 수면
                sleepDuration = sleepDuration,
                bedtime = bedtime,
                sleepQuality = sleepQuality,
                sleepAid = sleepAid,
                // Gated on SleepAid.Other so text typed then switched away doesn't leak.
                sleepAidOtherText = if (sleepAid == SleepAid.Other) sleepAidOtherText.trim().ifBlank { null } else null,
                // 스트레스 / 정신
                stressLevel = stressLevel,
                stressSources = stressSources.toSet(),
                relaxationActivities = relaxationActivities.toSet(),
            ),
            supplements = if (SurveySection.S5 in skipped) null else SupplementsSection(
                supplements = supplements.toSet(),
                supplementOtherText = supplementOtherText.trim().ifBlank { null },
                // Explicit 없음/있음 choice; null stays null (미입력) until the user picks.
                takingPrescription = takingPrescription,
                // Only persist 처방약 종류 when the user actually selected 있음.
                prescriptionText = if (takingPrescription == true) prescriptionNote.trim().ifBlank { null } else null,
                allergens = allergens.toSet(),
                allergenOtherText = allergenOtherText.trim().ifBlank { null },
            ),
            goals = GoalsSection(orderedGoals = rankedGoals(goalRanks)),
            budgetLifestyle = if (SurveySection.S7 in skipped) null else BudgetLifestyleSection(
                jobType = jobType,
                walkingTime = walkingTime,
                monthlyBudget = monthlyBudget,
                consultingInterest = consultingInterest,
            ),
            completedSections = completed,
            skippedSections = skipped.toSet(),
        )
    }
}

@Composable
fun rememberSurveyDraft(): SurveyDraft = remember { SurveyDraft() }
