# Calm Gamification of the Onboarding Survey

**Date:** 2026-06-08
**Status:** Approved design, pending implementation
**Branches:** new `feature/onboarding-gamified` off the PR #5 head (`feature/onboarding-survey-full`), since the full-survey work is not yet merged to develop.

## Goal

Make the onboarding survey interactive and pleasant to complete, borrowing Duolingo's **pacing and momentum** — explicitly NOT its playfulness or mascot. The signature move is **one-question-per-screen** with restrained reward feedback: the Oura/Noom interpretation of Duolingo, dressed in a trustworthy adult wellness aesthetic.

### Why "calm", not playful

The survey collects serious medical content — 질환·병력 (질환 history), 복용약 (medications), 알레르기 (allergens). A cheering cartoon mascot while a user selects "당뇨 (2형)" or "암 병력" reads as flippant and undermines the trust the sensitive-info Notice is built to establish. Duolingo can be silly because forgetting a Spanish word has no stakes; here the content is personal and sometimes heavy. So we take the pacing and momentum loop, and deliberately drop points, streaks, scores, and the mascot.

## Decisions (locked)

1. **Direction:** Calm gamification — one-question-per-screen flow + restrained momentum (animated progress, gentle checkpoints, warm micro-copy, satisfying finale). No mascot, no streaks/points.
2. **Granularity:** One field/group per screen. A single-select gets one screen; a multi-select *group* gets one screen with its chips (e.g. "심혈관·대사 질환이 있나요?" with its ~7 condition chips). ~35 light screens total including checkpoints — NOT one screen per chip (that would make it longer).
3. **Architecture:** Data-driven question registry (Option A) — questions are data, one generic screen renders any question by type, navigation/progress/validation are pure functions.

## Architecture

### Question model

A sealed `SurveyQuestion` type. Each question binds to an existing `SurveyDraft` field via getter/setter lambdas, so the draft stays the single source of truth (no parallel state):

```
sealed interface SurveyQuestion {
  val id: String              // stable key, e.g. "s2.cardio"
  val section: SurveySection  // for progress grouping + skip semantics
  val prompt: String          // big friendly question
  val helper: String?         // optional sub-text

  data class SingleChoice<T>(...) // enum, auto-advances on tap (gender, 혈압, mealCount...)
  data class MultiChoice<T>(...)  // Set toggle, explicit "계속" (condition groups, symptoms, diet restrictions...)
                                  //   carries optional conditionalText binding (e.g. 식품 알레르기 직접 입력 chip → free-text)
  data class Numeric(...)         // 키/체중/복부둘레, optional "모름" (unknownable)
  data class NumericPair(...)     // 키+체중 together (natural pair) on one screen
  data class Rating(...)          // 1..5 (수면의 질, 스트레스 수준)
  data class Ranked(...)          // S6 goals — keep tap-to-rank on one screen
  data class Info(...)            // non-input: sensitive-info Notice, section checkpoints (isCheckpoint), final reward (isReward)
}
```

`surveyQuestions: List<SurveyQuestion>` is the ordered flow (~35 items including checkpoints and the notice). This is the single source of flow order.

### Pure helpers (testable, no Compose)

Living alongside the registry (mirrors today's `SurveyStep.kt`):

- `nextQuestion(current)`, `prevQuestion(current)` — list navigation
- `progressFraction(current)` — monotonic 0f..1f
- `questionsInSection(section)` — for segmented progress
- `segmentProgress(current)` — per-section fill for the segmented bar
- `isAnswered(question, draft)` — drives "계속" enablement and auto-advance
- `sectionEyebrow(question)` — "S2 · 질환·병력 · 단계 3/8"

### Generic screen

One `QuestionScreen(question, draft, onNext, onBack, onSkip)` composable renders any `SurveyQuestion` by type:

- **Shared layout:** segmented progress bar at top → section eyebrow → big prompt (headlineSmall, ReverseEspresso) → optional helper → answer area (type-specific) → primary CTA ("계속") + skip link for skippable questions.
- **SingleChoice:** large tappable answer cards; tap selects (ReverseGold fill + check + spring scale-pop) and **auto-advances ~250ms later**.
- **MultiChoice:** same cards, tap toggles, no auto-advance; "계속" advances. Conditional free-text field appears below when its controlling chip is selected (reuses the gating already in `SurveyDraft.toResponse()`).
- **Numeric / NumericPair:** outlined number fields, optional "모름" chip (reuses the existing unknownable pattern).
- **Rating:** the existing 1..5 row, enlarged.
- **Ranked:** the existing tap-to-rank goal list, one screen.
- **Info:** prompt + body + single CTA. Checkpoints show a filling ring/badge; reward shows the trophy finale.

## Calm-gamification layer

1. **Segmented progress bar** — one segment per section (S0–S7); the active segment fills smoothly via `animateFloatAsState` (~300ms ease) as the user moves through its questions. Replaces today's single bar. Communicates "which chapter, how far in."
2. **Auto-advance on single-choice** — tap → ~250ms → advance (the Duolingo snap). Multi-choice/numeric require explicit "계속". Back always available.
3. **Selection feedback** — answer cards spring-scale-pop + fill transition + check mark on tap. The moment-to-moment delight.
4. **Section checkpoints** — a brief celebration screen after each section: filling ring/badge + warm, **content-aware** copy. Lighter sections get more celebration ("기본 정보 완료! 잘하고 있어요 ✨"); the medical section gets a calmer, respectful tone ("솔직하게 답해주셔서 감사해요"). One tap to continue. Modeled as `Info(isCheckpoint=true)` items.
5. **Finale** — the existing reward screen upgraded: trophy with a celebratory entrance, the full segmented bar filled, the first-month-benefit message, "홈으로 가기".
6. **Encouraging micro-copy** — prompts phrased as warm questions ("요즘 가장 신경 쓰이는 게 있나요?") rather than form labels.

### Tone guardrails (health-appropriate)

- No points, no streaks, no scores, no mascot.
- No confetti / loud celebration on medical-history screens (S2 질환, S3 증상, S5 복용약/알레르기). Celebration intensity is dialed down on those and up on lighter sections (S0 basic info, S6 goals).
- The sensitive-info Notice keeps its serious framing — it is not gamified.

### Accessibility

Correction from initial design: the app has **no** reduce-motion setting today (`LuminineSettings` = themeMode / highContrast / fontScale only). Rather than add a new toggle (scope creep into the settings model/repo/UI), the design keeps **all motion deliberately restrained** — short (≤300ms), subtle transitions (fades, fills, a small spring scale-pop), with no parallax, confetti physics, or looping animation. This is calm by construction and needs no opt-out. A central `private const val` animation-duration constant (and a single spec for the scale-pop) is used so motion can be globally toned down or disabled in one edit if a reduce-motion setting is added later. Font-scale and high-contrast settings continue to apply automatically (the generic screen uses theme colors + scalable type via the existing `LuminineTheme` density wiring).

## Testing

**Pure / unit-tested** (new `SurveyQuestionsTest`):
- Flow starts at the first basic-info question and ends at the reward.
- Every `SurveyQuestion` binds to a real `SurveyDraft` field (get/set round-trip).
- Skippable questions belong only to skippable sections (S2/S3/S4/S5/S7); required-section questions are not skippable.
- `progressFraction` is monotonic across the whole flow, in `0f..1f`, and `1f` at the reward.
- Checkpoints sit exactly at section boundaries; segmented-progress math is correct per section.
- `isAnswered` is correct per question type (e.g. a required single-choice is unanswered until set; a multi-choice is "answerable to empty").

**Unchanged tests:** all 21 existing `SurveyDraftMappingTest` cases stay green — the draft, model, and mapping are not modified.

**Not unit-tested:** the Compose UI (per this project's convention) — verified on-device on the Android emulator (and optionally the iOS sim), as done in the prior session.

## Files

**New:**
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt` — sealed model + `surveyQuestions` registry + pure helpers.
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/QuestionScreen.kt` — the generic screen + answer-card / checkpoint / reward composables.
- `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt`.

**Rewritten:**
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyFlow.kt` — becomes a thin driver: holds the draft + current-question index, renders `QuestionScreen`, handles next/back/skip/auto-advance, calls `onComplete(draft.toResponse())` at the reward. Shrinks dramatically as per-field UI moves into data + the generic screen.

**Unchanged:**
- `SurveyModels.kt`, `SurveyDraft.kt`, `SurveyPlan.kt`, `App.kt`, all repositories.
**Partially replaced:**
- `SurveyStep.kt` — verified references: `rankedGoals()` is used by `SurveyDraft.toResponse()` and tested, so it **stays** (moved into the new registry file or left in place — implementation's call). The rest of `SurveyStep.kt` — the `SurveyStep` flow enum, `nextStep`/`previousStep`/`progressFraction`/`stepLabel`/`countedSteps`/`surveyFlowOrder` — is used **only** by the old `SurveyFlow.kt` (being rewritten) and `SurveyStepTest.kt`. Those become dead and are removed, and the corresponding flow-only cases in `SurveyStepTest.kt` are removed; the two `rankedGoals` tests are kept (relocated to wherever `rankedGoals` lands).

## Scope guardrails (YAGNI)

Out of scope: backend, analytics events, A/B testing of copy, sound effects, haptics beyond Compose defaults, new persisted state (the draft already survives back/next), any new accessibility toggle / reduce-motion setting (motion is restrained by construction instead). The reward remains the existing first-month-benefit message — no new rewards system.

## Delivery

1. Branch `feature/onboarding-gamified` off the PR #5 head (`feature/onboarding-survey-full`).
2. TDD: write `SurveyQuestionsTest` red → build the registry + helpers green → build the generic screen + rewrite the driver → keep all existing tests green.
3. On-device QA on the Android emulator (drive the full flow, screenshot the new screens).
4. PR to `develop`. (main stays deploy-only.)
