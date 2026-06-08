package com.luminine.app.onboarding

import com.luminine.app.model.SurveySection

// PURE survey-wizard navigation + progress helpers. No Compose imports — unit-tested directly.
// Distinct from model.SurveySection (the persisted section key): this is the UI flow step,
// which also includes the sensitive-info Notice and the completion Reward.
enum class SurveyStep(
    val title: String,
    val skippable: Boolean,
    val isNotice: Boolean = false,
    val isReward: Boolean = false,
    val section: SurveySection? = null, // the persisted section this step edits (null for Notice/Reward)
) {
    S0("기본 인적사항", skippable = false, section = SurveySection.S0),
    S1("신체 기본정보", skippable = false, section = SurveySection.S1),
    Notice("민감정보 안내", skippable = false, isNotice = true),
    S2("질환·병력", skippable = true, section = SurveySection.S2),
    S3("체감 증상", skippable = true, section = SurveySection.S3),
    S4("생활습관", skippable = true, section = SurveySection.S4),
    S5("복용 중", skippable = true, section = SurveySection.S5),
    S6("관심영역 우선순위", skippable = false, section = SurveySection.S6),
    S7("라이프스타일 & 예산", skippable = true, section = SurveySection.S7),
    Reward("완료", skippable = false, isReward = true),
}

// Ordered flow exactly as the survey requires (enum declaration order).
val surveyFlowOrder: List<SurveyStep> = SurveyStep.entries

// The counted content steps for the "단계 n/8" progress label (excludes Notice + Reward).
val countedSteps: List<SurveyStep> = surveyFlowOrder.filter { !it.isNotice && !it.isReward }

fun nextStep(current: SurveyStep): SurveyStep? =
    surveyFlowOrder.getOrNull(surveyFlowOrder.indexOf(current) + 1)

fun previousStep(current: SurveyStep): SurveyStep? =
    surveyFlowOrder.getOrNull(surveyFlowOrder.indexOf(current) - 1)

// Monotonic 0f..1f progress. Reward = 1f. Notice fills the slot just before S2 (so the bar advances
// smoothly between S1 and S2). Counted steps fill (countedIndex + 1) / total.
fun progressFraction(current: SurveyStep): Float {
    if (current.isReward) return 1f
    val total = countedSteps.size
    if (current.isNotice) {
        // Notice sits between S1 and S2 — show S1's fraction (S2 not yet started).
        return countedSteps.indexOf(SurveyStep.S2).toFloat() / total
    }
    val idx = countedSteps.indexOf(current)
    return (idx + 1).toFloat() / total
}

// "단계 n/8" for counted steps; null for Notice/Reward (no label shown).
fun stepLabel(current: SurveyStep): String? {
    if (current.isNotice || current.isReward) return null
    val idx = countedSteps.indexOf(current)
    return "단계 ${idx + 1}/${countedSteps.size}"
}
