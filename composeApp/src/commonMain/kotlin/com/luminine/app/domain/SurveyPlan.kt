package com.luminine.app.domain

import com.luminine.app.model.ExerciseFrequency
import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.Routine
import com.luminine.app.model.RoutineCategory
import com.luminine.app.model.SurveyResponse
import com.luminine.app.model.SurveySection

// Pure domain logic: survey -> onboarding validation + Home seeding. No Compose, no IO.

// --------- VALIDATION ---------
// Required sections S0, S1, S6 must be "complete enough" to finish onboarding.
// S0: name non-blank, birthYear + gender + region present.
// S1: heightCm and weightKg present (others may be 모름/null).
// S6: at least one priority goal ranked.
fun isBasicInfoComplete(s: SurveyResponse): Boolean = with(s.basicInfo) {
    name.isNotBlank() && birthYear != null && gender != null && region != null
}

fun isBodyInfoComplete(s: SurveyResponse): Boolean = with(s.bodyInfo) {
    heightCm != null && weightKg != null
}

fun isGoalsComplete(s: SurveyResponse): Boolean = s.goals.orderedGoals.isNotEmpty()

fun canCompleteOnboarding(s: SurveyResponse): Boolean =
    isBasicInfoComplete(s) && isBodyInfoComplete(s) && isGoalsComplete(s)

// Which required sections are still missing (for UI hinting). Pure.
fun missingRequiredSections(s: SurveyResponse): Set<SurveySection> = buildSet {
    if (!isBasicInfoComplete(s)) add(SurveySection.S0)
    if (!isBodyInfoComplete(s)) add(SurveySection.S1)
    if (!isGoalsComplete(s)) add(SurveySection.S6)
}

// --------- SEEDING ---------
// Each goal pre-activates 1–2 RoutineCategories (primary + a supporting one).
// Used by the SampleData-preserving seeding below (seededRoutines), which keeps existing routine
// ids/order so Home/Charts/records keep working — we only flip isActive.
fun PriorityGoal.toCategories(): List<RoutineCategory> = when (this) {
    PriorityGoal.WeightBody -> listOf(RoutineCategory.Exercise, RoutineCategory.Diet)
    PriorityGoal.SkinAging -> listOf(RoutineCategory.SkinCare, RoutineCategory.InnerCare)
    PriorityGoal.EnergyVitality -> listOf(RoutineCategory.InnerCare, RoutineCategory.Exercise)
    PriorityGoal.MuscleMaintain -> listOf(RoutineCategory.Exercise, RoutineCategory.Diet)
    PriorityGoal.SleepQuality -> listOf(RoutineCategory.Sleep, RoutineCategory.Mind)
    PriorityGoal.Cognitive -> listOf(RoutineCategory.Mind, RoutineCategory.InnerCare)
    PriorityGoal.GutHealth -> listOf(RoutineCategory.Diet, RoutineCategory.InnerCare)
    PriorityGoal.HormoneBalance -> listOf(RoutineCategory.InnerCare, RoutineCategory.Sleep)
    PriorityGoal.Immunity -> listOf(RoutineCategory.InnerCare, RoutineCategory.Diet)
    PriorityGoal.JointPain -> listOf(RoutineCategory.Exercise, RoutineCategory.InnerCare)
}

// Which RoutineCategories should start active given the survey. Categories implied by the ranked
// goals (order preserved, deduped); S4 sedentary level (Never/Rarely) nudges Exercise in.
// Fallback: if no goals ranked, all categories stay active (current default behavior).
// Home content/category ordering is goal-driven (S6) by design. The doc's "체감 증상 → 홈 콘텐츠 카드
// 노출 순서" (S3) mapping is a documented post-MVP enhancement (S3 currently collects skin symptoms only).
fun seededCategories(survey: SurveyResponse): List<RoutineCategory> {
    val ordered = LinkedHashSet<RoutineCategory>()
    survey.goals.orderedGoals.forEach { it.toCategories().forEach(ordered::add) }
    when (survey.lifestyle?.exerciseFrequency) {
        ExerciseFrequency.Never, ExerciseFrequency.Rarely -> ordered.add(RoutineCategory.Exercise)
        else -> {}
    }
    return if (ordered.isEmpty()) RoutineCategory.entries.toList() else ordered.toList()
}

// Apply seeding to the default routine list: a routine isActive iff its category is seeded.
// Preserves existing Routine ids/order from SampleData.defaultRoutines so Home/Charts keep working.
fun seededRoutines(survey: SurveyResponse, defaults: List<Routine>): List<Routine> {
    val cats = seededCategories(survey).toSet()
    return defaults.map { it.copy(isActive = it.category in cats) }
}

// Greeting + avatar helpers (pure, tested) used by the Home top bar / menu profile.
fun greetingFor(displayName: String): String =
    if (displayName.isBlank()) "안녕하세요!" else "안녕하세요, ${displayName}님"

fun avatarInitial(displayName: String): String =
    displayName.trim().take(1).ifBlank { "L" }
