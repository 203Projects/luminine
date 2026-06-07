package com.luminine.app.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.luminine.app.model.AllergenComponent
import com.luminine.app.model.BasicInfoSection
import com.luminine.app.model.BodyInfoSection
import com.luminine.app.model.BudgetLifestyleSection
import com.luminine.app.model.ConditionsSection
import com.luminine.app.model.ConsultingInterest
import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.Gender
import com.luminine.app.model.GoalsSection
import com.luminine.app.model.JobType
import com.luminine.app.model.LifestyleSection
import com.luminine.app.model.MonthlyBudget
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Region
import com.luminine.app.model.SkinSymptom
import com.luminine.app.model.Supplement
import com.luminine.app.model.SupplementsSection
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection
import com.luminine.app.model.SymptomsSection
import com.luminine.app.model.WalkingTime

// Mutable in-progress survey holder, hoisted by SurveyFlow so each section's state survives
// back/next navigation. Compose-aware (mutableStateOf). Converts to the canonical, persisted
// SurveyResponse via toResponse(). NOT unit-tested directly (Compose state) — the pure mapping
// logic it relies on (rankedGoals, validity) lives in SurveyStep.kt and is tested there.
class SurveyDraft {
    // --- S0 기본 인적사항 (required) ---
    var name by mutableStateOf("")
    var birthYear by mutableStateOf<Int?>(null)
    var gender by mutableStateOf<Gender?>(null)
    var region by mutableStateOf<Region?>(null)

    // --- S1 신체 기본정보 (required: height + weight) ---
    var heightCm by mutableStateOf("")
    var weightKg by mutableStateOf("")
    var bodyFatPct by mutableStateOf("")
    var bodyFatUnknown by mutableStateOf(false)
    var muscleKg by mutableStateOf("")
    var muscleUnknown by mutableStateOf(false)

    // --- S2 질환·병력 (skippable) ---
    // MVP UI deferral: the model (ConditionsSection) captures the full 7-group taxonomy + per-item
    // allergy free-text, but onboarding currently collects a single free-text field to keep the
    // flow short. Structured group capture is a documented post-MVP enhancement.
    var conditionsCustom by mutableStateOf("")

    // --- S3 체감 증상 (skippable) ---
    // MVP UI deferral: SymptomsSection models all 8 symptom groups; onboarding collects the skin
    // group (the anti-aging headline concern). Remaining groups are a documented post-MVP addition.
    val skinSymptoms = mutableStateListOf<SkinSymptom>()

    // --- S4 생활습관 (skippable) — exercise drives Home seeding ---
    var exerciseFrequency by mutableStateOf<ExerciseFrequency?>(null)
    var sleepQuality by mutableStateOf<Int?>(null)
    var stressLevel by mutableStateOf<Int?>(null)

    // --- S5 복용 중 (skippable) ---
    val supplements = mutableStateListOf<Supplement>()
    var takingPrescription by mutableStateOf<Boolean?>(null) // null=미입력, false=없음, true=있음
    var prescriptionNote by mutableStateOf("")
    val allergens = mutableStateListOf<AllergenComponent>()

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

    // Validity gates for required-section "next" enablement.
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
                gender = gender,
                region = region,
            ),
            bodyInfo = BodyInfoSection(
                heightCm = heightCm.toDoubleOrNull(),
                weightKg = weightKg.toDoubleOrNull(),
                bodyFatPct = if (bodyFatUnknown) null else bodyFatPct.toDoubleOrNull(),
                muscleMassKg = if (muscleUnknown) null else muscleKg.toDoubleOrNull(),
            ),
            conditions = if (SurveySection.S2 in skipped) null else ConditionsSection(
                otherConditionText = conditionsCustom.trim().ifBlank { null },
            ),
            symptoms = if (SurveySection.S3 in skipped) null else SymptomsSection(
                skin = skinSymptoms.toSet(),
            ),
            lifestyle = if (SurveySection.S4 in skipped) null else LifestyleSection(
                exerciseFrequency = exerciseFrequency,
                sleepQuality = sleepQuality,
                stressLevel = stressLevel,
            ),
            supplements = if (SurveySection.S5 in skipped) null else SupplementsSection(
                supplements = supplements.toSet(),
                prescriptionText = prescriptionNote.trim().ifBlank { null },
                // Explicit 없음/있음 choice; null stays null (미입력) until the user picks.
                takingPrescription = takingPrescription,
                allergens = allergens.toSet(),
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
