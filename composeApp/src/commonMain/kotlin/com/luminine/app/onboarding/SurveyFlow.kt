package com.luminine.app.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.luminine.app.model.SurveyResponse

/**
 * One-question-per-screen onboarding survey (the Duolingo-paced, calm-gamification flow). The flow is
 * the data-driven [surveyQuestions] registry; [QuestionScreen] renders the current question; this
 * driver owns the draft + current-index state and the next/back/skip transitions. SingleChoice
 * auto-advances from within QuestionScreen. onComplete receives the canonical SurveyResponse.
 */
@Composable
fun SurveyFlow(
    modifier: Modifier = Modifier,
    onComplete: (SurveyResponse) -> Unit,
) {
    val draft = rememberSurveyDraft()
    var index by remember { mutableStateOf(0) }
    val current = surveyQuestions[index]

    fun goNext() {
        val next = nextQuestion(current)
        if (next == null) onComplete(draft.toResponse()) else index = surveyQuestions.indexOf(next)
    }
    fun goBack() { prevQuestion(current)?.let { index = surveyQuestions.indexOf(it) } }
    fun skip() {
        draft.markSkipped(current.section)
        // "나중에 입력" skips the REST of this section (and its checkpoint) — jump to the next
        // section's first question, matching the old section-level skip intent.
        val nextDiff = surveyQuestions.drop(index + 1).firstOrNull { it.section != current.section }
        if (nextDiff == null) onComplete(draft.toResponse()) else index = surveyQuestions.indexOf(nextDiff)
    }

    QuestionScreen(
        modifier = modifier.fillMaxSize().safeDrawingPadding(),
        question = current,
        draft = draft,
        onNext = ::goNext,
        onBack = if (index == 0) null else ::goBack,
        onSkip = if (current.skippable) ::skip else null,
    )
}
