package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection

// PURE survey-question registry + navigation/progress helpers. No Compose imports — unit-tested
// directly (mirrors the discipline of the former SurveyStep.kt). The gamified one-question-per-screen
// flow is THIS list; the generic QuestionScreen composable renders each item by type.

// A single question/screen in the flow. Binds to a SurveyDraft field via get/set lambdas so the
// draft stays the single source of truth — no parallel state.
sealed interface SurveyQuestion {
    val id: String                 // stable key, e.g. "s2.cardio"
    val section: SurveySection     // for progress grouping + skip semantics
    val prompt: String             // big friendly question
    val helper: String?            // optional sub-text
    val skippable: Boolean         // shows "나중에 입력" (only true on skippable sections)

    // Single-choice enum — auto-advances on tap. nullable selection (null = unanswered).
    data class SingleChoice<T : Enum<T>>(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val options: List<T>,
        val labelOf: (T) -> String,
        val get: (SurveyDraft) -> T?,
        val set: (SurveyDraft, T) -> Unit,
        val required: Boolean = false, // gates flow completion for required sections
    ) : SurveyQuestion

    // Multi-choice Set — tap to toggle, explicit "계속". Optional conditional free-text.
    data class MultiChoice<T : Enum<T>>(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val options: List<T>,
        val labelOf: (T) -> String,
        val selected: (SurveyDraft) -> MutableList<T>, // the draft's SnapshotStateList
        val conditional: ConditionalText<T>? = null,
    ) : SurveyQuestion

    // Numeric input (키/체중/복부둘레), optionally "모름"-able.
    data class Numeric(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val unit: String,                 // "cm", "kg", "%"
        val unknownable: Boolean = false,
        val get: (SurveyDraft) -> String,
        val set: (SurveyDraft, String) -> Unit,
        val unknownGet: (SurveyDraft) -> Boolean = { false },
        val unknownSet: (SurveyDraft, Boolean) -> Unit = { _, _ -> },
        val required: Boolean = false,
    ) : SurveyQuestion

    // Two numerics on one screen (키 + 체중 — a natural pair; both required to advance).
    data class NumericPair(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val leftLabel: String, val leftGet: (SurveyDraft) -> String, val leftSet: (SurveyDraft, String) -> Unit,
        val rightLabel: String, val rightGet: (SurveyDraft) -> String, val rightSet: (SurveyDraft, String) -> Unit,
        val required: Boolean = true,
    ) : SurveyQuestion

    // 1..5 rating (수면의 질, 스트레스 수준).
    data class Rating(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val get: (SurveyDraft) -> Int?,
        val set: (SurveyDraft, Int) -> Unit,
    ) : SurveyQuestion

    // Ranked goals (S6) — tap-to-rank, one screen. Required (≥1).
    data class Ranked(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
    ) : SurveyQuestion

    // S0 name + birthdate + gender + region — a bespoke mixed screen (text + number + enum).
    data class Identity(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
    ) : SurveyQuestion

    // Non-input screen: the sensitive-info Notice, section checkpoints, the final reward.
    data class Info(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
        val body: String,
        val ctaLabel: String,
        val kind: InfoKind,
    ) : SurveyQuestion
}

enum class InfoKind { Notice, Checkpoint, Reward }

// Conditional free-text shown when [whenSelected] is in the multi-choice set.
data class ConditionalText<T>(
    val whenSelected: T,
    val label: String,
    val get: (SurveyDraft) -> String,
    val set: (SurveyDraft, String) -> Unit,
)

// S6 ranked-goal ordering (relocated verbatim from SurveyStep.kt): goal -> 1-based rank -> ordered list.
fun rankedGoals(ranks: Map<PriorityGoal, Int>): List<PriorityGoal> =
    ranks.entries.sortedBy { it.value }.map { it.key }
