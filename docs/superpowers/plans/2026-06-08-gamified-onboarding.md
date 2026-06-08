# Gamified Onboarding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure the onboarding survey into a Duolingo-paced, one-question-per-screen flow with restrained, health-appropriate momentum/reward feedback — driven by a data registry of questions rendered by a single generic screen.

**Architecture:** A sealed `SurveyQuestion` model + an ordered `surveyQuestions` registry binds each question to an existing `SurveyDraft` field via get/set lambdas. Pure navigation/progress/validation helpers (unit-tested, no Compose). One generic `QuestionScreen` composable renders any question by type. `SurveyFlow` becomes a thin driver over the registry. `SurveyModels.kt` / `SurveyDraft.kt` / the mapping are untouched, so all 21 existing `SurveyDraftMappingTest` cases stay green.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform (Material 3), kotlin.test. Build dir `/Users/hanshin/Documents/Projects/luminine`. Branch `feature/onboarding-gamified` (already created off the PR #5 head). Test cmd: `./gradlew :composeApp:allTests --rerun-tasks` (force rerun — Gradle config-cache masks source edits on this project). Compile-only: `./gradlew :composeApp:compileKotlinMetadata`.

**Reference:** spec at `docs/superpowers/specs/2026-06-08-gamified-onboarding-design.md`.

---

## File Structure

**New:**
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt` — sealed `SurveyQuestion` model, `surveyQuestions` registry, pure helpers (`nextQuestion`/`prevQuestion`/`progressFraction`/`segmentInfo`/`isAnswered`/`sectionEyebrow`), and the relocated `rankedGoals`.
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/QuestionScreen.kt` — generic `QuestionScreen` + answer-card / numeric / rating / ranked / checkpoint / reward composables + animation constants.
- `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt` — pure-logic tests.

**Rewritten:**
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyFlow.kt` — thin driver: draft + current-index state, renders `QuestionScreen`, handles next/back/skip/auto-advance, calls `onComplete(draft.toResponse())` at the reward.

**Partially edited / dead-code removed:**
- `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyStep.kt` — remove the `SurveyStep` flow enum + `surveyFlowOrder`/`countedSteps`/`nextStep`/`previousStep`/`progressFraction`/`stepLabel` (used only by old `SurveyFlow`). `rankedGoals` moves to `SurveyQuestions.kt`. The persisted `model.SurveySection` enum is in `SurveyModels.kt` and is untouched.
- `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyStepTest.kt` — remove flow-only tests; keep the two `rankedGoals` tests (relocated to `SurveyQuestionsTest.kt`).

**Untouched:** `SurveyModels.kt`, `SurveyDraft.kt`, `SurveyPlan.kt`, `App.kt`, all repositories, `SurveyDraftMappingTest.kt`.

---

## Task 1: Question model + registry skeleton (compiles, no behavior yet)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt`

- [ ] **Step 1: Create the sealed model + a minimal registry + relocate `rankedGoals`.**

Write `SurveyQuestions.kt`. The model binds to `SurveyDraft` via lambdas. Start with the model, helpers stubbed, and a SMALL registry (S0 questions only) to prove the shape compiles; later tasks grow the registry.

```kotlin
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
```

- [ ] **Step 2: Verify it compiles.**

Run: `cd /Users/hanshin/Documents/Projects/luminine && ./gradlew :composeApp:compileKotlinMetadata --console=plain 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`. (Registry is empty for now; `rankedGoals` now exists in two files — that's fine until Task 6 removes the old one. If a duplicate-declaration error appears, it means both files export `rankedGoals` in the same package — proceed to Step 3 to remove the old one early.)

- [ ] **Step 3: Remove `rankedGoals` from `SurveyStep.kt` to avoid the duplicate.**

In `SurveyStep.kt`, delete the `rankedGoals` function (the `fun rankedGoals(...)` block at the bottom) — it now lives in `SurveyQuestions.kt`. Leave the rest of `SurveyStep.kt` for now (Task 6 removes the dead flow helpers).

- [ ] **Step 4: Compile again.**

Run: `./gradlew :composeApp:compileKotlinMetadata --console=plain 2>&1 | tail -5`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit.**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt \
        composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyStep.kt
git commit -m "feat: SurveyQuestion model + registry skeleton; relocate rankedGoals"
```

---

## Task 2: Build the full question registry (data only)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt`

- [ ] **Step 1: Write the failing test for registry coverage.**

Create `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt`:

```kotlin
package com.luminine.app.onboarding

import com.luminine.app.model.PriorityGoal
import com.luminine.app.model.SurveySection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SurveyQuestionsTest {
    @Test fun registryIsNonEmptyAndStartsWithBasicInfo() {
        assertTrue(surveyQuestions.isNotEmpty())
        assertEquals(SurveySection.S0, surveyQuestions.first().section)
    }

    @Test fun registryEndsWithTheRewardInfoScreen() {
        val last = surveyQuestions.last()
        assertTrue(last is SurveyQuestion.Info && last.kind == InfoKind.Reward)
    }

    @Test fun everySectionIsRepresented() {
        val sections = surveyQuestions.map { it.section }.toSet()
        assertEquals(SurveySection.entries.toSet(), sections)
    }

    @Test fun onlySkippableSectionsHaveSkippableQuestions() {
        val skippableSections = setOf(
            SurveySection.S2, SurveySection.S3, SurveySection.S4, SurveySection.S5, SurveySection.S7,
        )
        surveyQuestions.filter { it.skippable }.forEach {
            assertTrue(it.section in skippableSections, "${it.id} skippable but section ${it.section} is required")
        }
    }

    @Test fun questionIdsAreUnique() {
        val ids = surveyQuestions.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "duplicate question id present")
    }

    @Test fun rankedGoalsSortsByRankAscending() {
        val ranks = mapOf(
            PriorityGoal.SleepQuality to 2, PriorityGoal.SkinAging to 1, PriorityGoal.MuscleMaintain to 3,
        )
        assertEquals(
            listOf(PriorityGoal.SkinAging, PriorityGoal.SleepQuality, PriorityGoal.MuscleMaintain),
            rankedGoals(ranks),
        )
    }

    @Test fun rankedGoalsEmptyWhenNoSelection() {
        assertTrue(rankedGoals(emptyMap()).isEmpty())
    }
}
```

- [ ] **Step 2: Run to verify it fails.**

Run: `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --rerun-tasks --console=plain 2>&1 | grep -iE "\.kt:|error|BUILD"`
Expected: FAIL — `surveyQuestions` is unresolved.

- [ ] **Step 3: Implement the full registry.**

Append to `SurveyQuestions.kt` the `surveyQuestions` list. Build it section by section, binding to the EXACT `SurveyDraft` field names (verify against `SurveyDraft.kt`). Use the model enums' `.label` for `labelOf`. Insert `Info(Checkpoint)` after each section's questions and `Info(Notice)` before S2. Prompts are warm questions. Below is the complete list — reproduce it fully (excerpt shows the pattern for every type; fill ALL fields per the gap list in the spec):

```kotlin
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
import com.luminine.app.model.SurveySection.S0
import com.luminine.app.model.SurveySection.S1
import com.luminine.app.model.SurveySection.S2
import com.luminine.app.model.SurveySection.S3
import com.luminine.app.model.SurveySection.S4
import com.luminine.app.model.SurveySection.S5
import com.luminine.app.model.SurveySection.S6
import com.luminine.app.model.SurveySection.S7
import com.luminine.app.model.VisionStatus
import com.luminine.app.model.WalkingTime
import com.luminine.app.model.WaterIntake

val surveyQuestions: List<SurveyQuestion> = buildList {
    // ---------- S0 기본 인적사항 (required) ----------
    // Name is a text field; model it as a dedicated Numeric-like text question OR keep name+birth on
    // one screen. DECISION: keep S0 identity (name + birth year/month/day) on ONE NumericPair-style
    // screen is awkward; instead add a small TextQuestion. To avoid a new type, reuse Numeric with a
    // non-numeric keyboard is wrong. SIMPLEST: S0 stays a SHORT multi-field screen rendered as a
    // special-case. Implement S0 identity as a dedicated Info-less composite — see Task 4 note.
    // For the registry, represent S0 identity as a single SingleChoice-free entry of a new lightweight
    // type is overkill. Pragmatic rule: model S0 name+birth as ONE Numeric-family screen is not valid.
    //
    // RESOLUTION (locked): add a 6th input type `Identity` for the S0 name+birthdate+gender+region
    // screen — it is genuinely unique (mixed text/number/enum) and trying to force it into the generic
    // types hurts more than one bespoke screen. Add to the sealed interface:
    //   data class Identity(override id/section/prompt/helper, skippable=false) : SurveyQuestion
    add(SurveyQuestion.Identity(id = "s0.identity", section = S0, prompt = "먼저, 본인을 알려주세요", helper = "맞춤 추천의 시작이에요"))
    add(checkpoint("s0.done", S0, "기본 정보 완료! 시작이 좋아요 ✨"))

    // ---------- S1 신체 기본정보 (required height+weight) ----------
    add(SurveyQuestion.NumericPair(
        id = "s1.htwt", section = S1, prompt = "키와 체중을 알려주세요",
        leftLabel = "키 (cm)", leftGet = { it.heightCm }, leftSet = { d, v -> d.heightCm = v },
        rightLabel = "체중 (kg)", rightGet = { it.weightKg }, rightSet = { d, v -> d.weightKg = v },
    ))
    add(SurveyQuestion.Numeric(id = "s1.bodyfat", section = S1, prompt = "체지방률을 아시나요?", unit = "%",
        unknownable = true, get = { it.bodyFatPct }, set = { d, v -> d.bodyFatPct = v },
        unknownGet = { it.bodyFatUnknown }, unknownSet = { d, b -> d.bodyFatUnknown = b; if (b) d.bodyFatPct = "" }))
    add(SurveyQuestion.Numeric(id = "s1.muscle", section = S1, prompt = "근육량을 아시나요?", unit = "kg",
        unknownable = true, get = { it.muscleKg }, set = { d, v -> d.muscleKg = v },
        unknownGet = { it.muscleUnknown }, unknownSet = { d, b -> d.muscleUnknown = b; if (b) d.muscleKg = "" }))
    add(SurveyQuestion.Numeric(id = "s1.waist", section = S1, prompt = "복부둘레를 아시나요?", unit = "cm",
        unknownable = true, get = { it.waistCm }, set = { d, v -> d.waistCm = v },
        unknownGet = { it.waistUnknown }, unknownSet = { d, b -> d.waistUnknown = b; if (b) d.waistCm = "" }))
    add(SurveyQuestion.SingleChoice(id = "s1.bp", section = S1, prompt = "혈압은 어떤가요?",
        options = BloodPressureStatus.entries, labelOf = { it.label }, get = { it.bloodPressure }, set = { d, v -> d.bloodPressure = v }))
    add(SurveyQuestion.SingleChoice(id = "s1.bs", section = S1, prompt = "혈당은 어떤가요?",
        options = BloodSugarStatus.entries, labelOf = { it.label }, get = { it.bloodSugar }, set = { d, v -> d.bloodSugar = v }))
    add(SurveyQuestion.SingleChoice(id = "s1.vision", section = S1, prompt = "시력은 어떤가요?",
        options = VisionStatus.entries, labelOf = { it.label }, get = { it.vision }, set = { d, v -> d.vision = v }))
    add(SurveyQuestion.SingleChoice(id = "s1.hearing", section = S1, prompt = "청력은 어떤가요?",
        options = HearingStatus.entries, labelOf = { it.label }, get = { it.hearing }, set = { d, v -> d.hearing = v }))
    add(checkpoint("s1.done", S1, "신체 정보 완료! 잘하고 있어요"))

    // ---------- Notice before S2 ----------
    add(SurveyQuestion.Info(id = "notice", section = S2, prompt = "민감정보 안내",
        body = "입력하신 건강 정보는 맞춤 추천에만 사용되며, 동의 없이 제3자에게 제공되지 않습니다. 이어지는 질환·증상·복용 정보는 모두 건너뛸 수 있어요.",
        ctaLabel = "이해했어요", kind = InfoKind.Notice))

    // ---------- S2 질환·병력 (skippable) — one screen per group ----------
    add(SurveyQuestion.MultiChoice(id = "s2.cardio", section = S2, prompt = "심혈관·대사 질환이 있나요?",
        helper = "해당하는 항목을 모두 선택하세요", skippable = true,
        options = CardioMetabolicCondition.entries, labelOf = { it.label }, selected = { it.cardioMetabolic }))
    add(SurveyQuestion.MultiChoice(id = "s2.digestive", section = S2, prompt = "소화기 질환이 있나요?", skippable = true,
        options = DigestiveCondition.entries, labelOf = { it.label }, selected = { it.digestiveConditions }))
    add(SurveyQuestion.MultiChoice(id = "s2.musculo", section = S2, prompt = "근골격 질환이 있나요?", skippable = true,
        options = MusculoskeletalCondition.entries, labelOf = { it.label }, selected = { it.musculoskeletal }))
    add(SurveyQuestion.MultiChoice(id = "s2.hormone", section = S2, prompt = "호르몬·내분비 질환이 있나요?", skippable = true,
        options = HormoneCondition.entries, labelOf = { it.label }, selected = { it.hormoneConditions }))
    add(SurveyQuestion.MultiChoice(id = "s2.neuro", section = S2, prompt = "신경·정신 관련 질환이 있나요?", skippable = true,
        options = NeuroPsychCondition.entries, labelOf = { it.label }, selected = { it.neuroPsych }))
    add(SurveyQuestion.MultiChoice(id = "s2.immune", section = S2, prompt = "면역·알레르기 질환이 있나요?", skippable = true,
        options = ImmuneAllergyCondition.entries, labelOf = { it.label }, selected = { it.immuneAllergy },
        conditional = ConditionalText(ImmuneAllergyCondition.FoodAllergy, "식품 알레르기 직접 입력",
            { it.foodAllergyText }, { d, v -> d.foodAllergyText = v })))
    // Note: the 면역 group has TWO conditional texts (food + environmental). The model supports one
    // `conditional`. Render the second (environmental) inside QuestionScreen by also checking
    // ImmuneAllergyCondition.EnvironmentalAllergy directly. Documented in Task 3.
    add(SurveyQuestion.MultiChoice(id = "s2.other", section = S2, prompt = "그 밖에 해당하는 질환이 있나요?", skippable = true,
        options = OtherCondition.entries, labelOf = { it.label }, selected = { it.otherConditions }))
    add(checkpoint("s2.done", S2, "솔직하게 답해주셔서 감사해요", calm = true))

    // ---------- S3 체감 증상 (skippable) — one screen per group ----------
    add(SurveyQuestion.MultiChoice(id = "s3.energy", section = S3, prompt = "에너지·피로, 어떤가요?", skippable = true,
        options = EnergySymptom.entries, labelOf = { it.label }, selected = { it.energySymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.body", section = S3, prompt = "체형·체중 관련 고민이 있나요?", skippable = true,
        options = BodyShapeSymptom.entries, labelOf = { it.label }, selected = { it.bodyShapeSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.skin", section = S3, prompt = "피부·외모 관련 고민이 있나요?", skippable = true,
        options = SkinSymptom.entries, labelOf = { it.label }, selected = { it.skinSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.digest", section = S3, prompt = "소화·장은 어떤가요?", skippable = true,
        options = DigestiveSymptom.entries, labelOf = { it.label }, selected = { it.digestiveSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.sleep", section = S3, prompt = "수면은 어떤가요?", skippable = true,
        options = SleepSymptom.entries, labelOf = { it.label }, selected = { it.sleepSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.cognitive", section = S3, prompt = "정신·인지 관련 고민이 있나요?", skippable = true,
        options = CognitiveSymptom.entries, labelOf = { it.label }, selected = { it.cognitiveSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.hormonal", section = S3, prompt = "호르몬·성 관련 고민이 있나요?", skippable = true,
        options = HormonalSymptom.entries, labelOf = { it.label }, selected = { it.hormonalSymptoms }))
    add(SurveyQuestion.MultiChoice(id = "s3.joint", section = S3, prompt = "관절·통증은 어떤가요?", skippable = true,
        options = JointPainSymptom.entries, labelOf = { it.label }, selected = { it.jointPainSymptoms }))
    add(checkpoint("s3.done", S3, "거의 절반 왔어요!"))

    // ---------- S4 생활습관 (skippable) ----------
    add(SurveyQuestion.SingleChoice(id = "s4.meals", section = S4, prompt = "하루 몇 끼 드시나요?", skippable = true,
        options = MealCount.entries, labelOf = { it.label }, get = { it.mealCount }, set = { d, v -> d.mealCount = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.regularity", section = S4, prompt = "식사는 규칙적인가요?", skippable = true,
        options = MealRegularity.entries, labelOf = { it.label }, get = { it.mealRegularity }, set = { d, v -> d.mealRegularity = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.staple", section = S4, prompt = "주식 유형은 무엇인가요?", skippable = true,
        options = StapleDietType.entries, labelOf = { it.label }, get = { it.stapleDietType }, set = { d, v -> d.stapleDietType = v }))
    add(SurveyQuestion.MultiChoice(id = "s4.diet", section = S4, prompt = "식이 제한이 있나요?", skippable = true,
        options = DietRestriction.entries, labelOf = { it.label }, selected = { it.dietRestrictions }))
    add(SurveyQuestion.SingleChoice(id = "s4.water", section = S4, prompt = "하루 수분 섭취량은요?", skippable = true,
        options = WaterIntake.entries, labelOf = { it.label }, get = { it.waterIntake }, set = { d, v -> d.waterIntake = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.alcohol", section = S4, prompt = "음주 빈도는요?", skippable = true,
        options = AlcoholFrequency.entries, labelOf = { it.label }, get = { it.alcoholFrequency }, set = { d, v -> d.alcoholFrequency = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.smoking", section = S4, prompt = "흡연은 어떻게 되나요?", skippable = true,
        options = SmokingStatus.entries, labelOf = { it.label }, get = { it.smokingStatus }, set = { d, v -> d.smokingStatus = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.caffeine", section = S4, prompt = "카페인은 얼마나 드시나요?", skippable = true,
        options = CaffeineIntake.entries, labelOf = { it.label }, get = { it.caffeineIntake }, set = { d, v -> d.caffeineIntake = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.exfreq", section = S4, prompt = "운동은 얼마나 자주 하나요?", skippable = true,
        options = ExerciseFrequency.entries, labelOf = { it.label }, get = { it.exerciseFrequency }, set = { d, v -> d.exerciseFrequency = v }))
    add(SurveyQuestion.MultiChoice(id = "s4.extype", section = S4, prompt = "어떤 운동을 하나요?", skippable = true,
        options = ExerciseType.entries, labelOf = { it.label }, selected = { it.exerciseTypes }))
    add(SurveyQuestion.SingleChoice(id = "s4.exint", section = S4, prompt = "운동 강도는요?", skippable = true,
        options = ExerciseIntensity.entries, labelOf = { it.label }, get = { it.exerciseIntensity }, set = { d, v -> d.exerciseIntensity = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.exdur", section = S4, prompt = "1회 운동 시간은요?", skippable = true,
        options = ExerciseDuration.entries, labelOf = { it.label }, get = { it.exerciseDuration }, set = { d, v -> d.exerciseDuration = v }))
    add(SurveyQuestion.MultiChoice(id = "s4.exgoal", section = S4, prompt = "운동 목표는 무엇인가요?", skippable = true,
        options = ExerciseGoal.entries, labelOf = { it.label }, selected = { it.exerciseGoals }))
    add(SurveyQuestion.SingleChoice(id = "s4.sleepdur", section = S4, prompt = "평균 수면 시간은요?", skippable = true,
        options = SleepDuration.entries, labelOf = { it.label }, get = { it.sleepDuration }, set = { d, v -> d.sleepDuration = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.bedtime", section = S4, prompt = "보통 몇 시에 주무시나요?", skippable = true,
        options = BedtimeRange.entries, labelOf = { it.label }, get = { it.bedtime }, set = { d, v -> d.bedtime = v }))
    add(SurveyQuestion.Rating(id = "s4.sleepq", section = S4, prompt = "수면의 질은 어떤가요?", skippable = true,
        get = { it.sleepQuality }, set = { d, v -> d.sleepQuality = v }))
    add(SurveyQuestion.SingleChoice(id = "s4.sleepaid", section = S4, prompt = "수면 보조제를 쓰나요?", skippable = true,
        options = SleepAid.entries, labelOf = { it.label }, get = { it.sleepAid }, set = { d, v -> d.sleepAid = v }))
    add(SurveyQuestion.Rating(id = "s4.stress", section = S4, prompt = "스트레스 수준은 어떤가요?", skippable = true,
        get = { it.stressLevel }, set = { d, v -> d.stressLevel = v }))
    add(SurveyQuestion.MultiChoice(id = "s4.stresssrc", section = S4, prompt = "주요 스트레스 원인은요?", skippable = true,
        options = StressSource.entries, labelOf = { it.label }, selected = { it.stressSources }))
    add(SurveyQuestion.MultiChoice(id = "s4.relax", section = S4, prompt = "어떻게 이완하나요?", skippable = true,
        options = RelaxationActivity.entries, labelOf = { it.label }, selected = { it.relaxationActivities }))
    add(checkpoint("s4.done", S4, "라이프스타일까지 완료! 멋져요"))

    // ---------- S5 복용 중 (skippable) ----------
    add(SurveyQuestion.MultiChoice(id = "s5.supps", section = S5, prompt = "복용 중인 영양제가 있나요?", skippable = true,
        options = Supplement.entries, labelOf = { it.label }, selected = { it.supplements }))
    add(SurveyQuestion.MultiChoice(id = "s5.allergen", section = S5, prompt = "알레르기 성분이 있나요?", skippable = true,
        options = AllergenComponent.entries, labelOf = { it.label }, selected = { it.allergens }))
    // 처방약 여부 (있음/없음 + conditional note) is a small bespoke screen — implement as Info-less
    // special case in QuestionScreen keyed by id "s5.prescription". Documented in Task 4.
    add(SurveyQuestion.Info(id = "s5.prescription", section = S5, prompt = "처방약을 복용 중인가요?",
        body = "영양제 추천 충돌을 막기 위해 사용돼요.", ctaLabel = "계속", kind = InfoKind.Notice)) // rendered specially; see Task 4
    add(checkpoint("s5.done", S5, "거의 다 왔어요"))

    // ---------- S6 관심영역 우선순위 (required) ----------
    add(SurveyQuestion.Ranked(id = "s6.goals", section = S6,
        prompt = "가장 중요한 목표는 무엇인가요?", helper = "탭한 순서대로 1·2·3순위가 매겨져요"))
    add(checkpoint("s6.done", S6, "목표 설정 완료!"))

    // ---------- S7 라이프스타일 & 예산 (skippable) ----------
    add(SurveyQuestion.SingleChoice(id = "s7.job", section = S7, prompt = "직업 유형은요?", skippable = true,
        options = JobType.entries, labelOf = { it.label }, get = { it.jobType }, set = { d, v -> d.jobType = v }))
    add(SurveyQuestion.SingleChoice(id = "s7.walk", section = S7, prompt = "하루 평균 보행 시간은요?", skippable = true,
        options = WalkingTime.entries, labelOf = { it.label }, get = { it.walkingTime }, set = { d, v -> d.walkingTime = v }))
    add(SurveyQuestion.SingleChoice(id = "s7.budget", section = S7, prompt = "건강관리 월 예산은요?", skippable = true,
        options = MonthlyBudget.entries, labelOf = { it.label }, get = { it.monthlyBudget }, set = { d, v -> d.monthlyBudget = v }))
    add(SurveyQuestion.SingleChoice(id = "s7.consult", section = S7, prompt = "1:1 전문 컨설팅에 관심 있나요?", skippable = true,
        options = ConsultingInterest.entries, labelOf = { it.label }, get = { it.consultingInterest }, set = { d, v -> d.consultingInterest = v }))

    // ---------- Reward ----------
    add(SurveyQuestion.Info(id = "reward", section = S7, prompt = "설문 완료!",
        body = "첫 달 혜택이 적용되었어요 — 오늘부터 나에게 맞춘 루틴을 시작해보세요.",
        ctaLabel = "홈으로 가기", kind = InfoKind.Reward))
}

// Checkpoint factory. `calm = true` tones down the celebration for medical sections.
private fun checkpoint(id: String, section: com.luminine.app.model.SurveySection, message: String, calm: Boolean = false) =
    SurveyQuestion.Info(id = id, section = section, prompt = if (calm) "" else "잘하고 있어요",
        body = message, ctaLabel = "계속", kind = InfoKind.Checkpoint)
```

Also add the `Identity` type to the sealed interface (per the locked resolution in the S0 comment):

```kotlin
    // S0 name + birthdate + gender + region — a bespoke mixed screen (text + number + enum).
    data class Identity(
        override val id: String,
        override val section: SurveySection,
        override val prompt: String,
        override val helper: String? = null,
        override val skippable: Boolean = false,
    ) : SurveyQuestion
```

- [ ] **Step 4: Run the registry tests — expect PASS.**

Run: `./gradlew :composeApp:allTests --rerun-tasks --console=plain 2>&1 | grep -E "SurveyQuestionsTest|FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, `SurveyQuestionsTest` cases pass, existing tests still green.

- [ ] **Step 5: Commit.**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt
git commit -m "feat: full survey question registry (one-field-per-screen)"
```

---

## Task 3: Pure navigation + progress + answered helpers

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt`

- [ ] **Step 1: Write the failing tests for the helpers.**

Append to `SurveyQuestionsTest.kt`:

```kotlin
import com.luminine.app.model.SurveySection as Sec

class SurveyNavTest {
    private val first = surveyQuestions.first()
    private val last = surveyQuestions.last()

    @Test fun prevOfFirstIsNullAndNextOfLastIsNull() {
        assertEquals(null, prevQuestion(first))
        assertEquals(null, nextQuestion(last))
    }

    @Test fun nextAndPrevAreInverse() {
        val q = surveyQuestions[3]
        assertEquals(q, prevQuestion(nextQuestion(q)!!))
    }

    @Test fun progressIsMonotonicAndBounded() {
        var lastF = -1f
        surveyQuestions.forEach { q ->
            val f = progressFraction(q)
            assertTrue(f in 0f..1f, "${q.id} out of range: $f")
            assertTrue(f >= lastF, "${q.id} regressed: $f < $lastF")
            lastF = f
        }
        assertEquals(1f, progressFraction(last))
    }

    @Test fun segmentInfoCoversEightSections() {
        // segmentInfo returns (sectionIndex 0..7, fractionWithinSection 0..1).
        val (idx0, _) = segmentInfo(first)
        assertEquals(0, idx0)
        val (idxLast, fracLast) = segmentInfo(last)
        assertEquals(7, idxLast) // reward is in S7
        assertEquals(1f, fracLast)
    }

    @Test fun isAnsweredFalseForUntouchedRequiredSingleChoice() {
        val draft = SurveyDraft()
        val q = surveyQuestions.first { it is SurveyQuestion.SingleChoice<*> } as SurveyQuestion.SingleChoice<*>
        assertEquals(false, isAnswered(q, draft))
    }

    @Test fun infoScreensAreAlwaysAnswered() {
        val info = surveyQuestions.first { it is SurveyQuestion.Info }
        assertTrue(isAnswered(info, SurveyDraft()))
    }
}
```

- [ ] **Step 2: Run to verify it fails.**

Run: `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --rerun-tasks --console=plain 2>&1 | grep -iE "\.kt:|error|BUILD"`
Expected: FAIL — `nextQuestion`/`prevQuestion`/`progressFraction`/`segmentInfo`/`isAnswered` unresolved.

- [ ] **Step 3: Implement the helpers in `SurveyQuestions.kt`.**

```kotlin
import com.luminine.app.model.SurveySection

fun nextQuestion(current: SurveyQuestion): SurveyQuestion? =
    surveyQuestions.getOrNull(surveyQuestions.indexOf(current) + 1)

fun prevQuestion(current: SurveyQuestion): SurveyQuestion? =
    surveyQuestions.getOrNull(surveyQuestions.indexOf(current) - 1)

// Monotonic 0f..1f over the whole flow by position.
fun progressFraction(current: SurveyQuestion): Float {
    val i = surveyQuestions.indexOf(current)
    return (i + 1).toFloat() / surveyQuestions.size
}

private val sectionOrder = SurveySection.entries // S0..S7 in declaration order

// (sectionIndex 0..7, fraction-through-that-section 0f..1f) for the segmented progress bar.
fun segmentInfo(current: SurveyQuestion): Pair<Int, Float> {
    val sec = current.section
    val idx = sectionOrder.indexOf(sec)
    val inSection = surveyQuestions.filter { it.section == sec }
    val pos = inSection.indexOf(current)
    val frac = (pos + 1).toFloat() / inSection.size
    return idx to frac
}

fun isAnswered(q: SurveyQuestion, draft: SurveyDraft): Boolean = when (q) {
    is SurveyQuestion.Info -> true
    is SurveyQuestion.SingleChoice<*> -> !q.required || q.get(draft) != null
    is SurveyQuestion.MultiChoice<*> -> true // multi can be legitimately empty
    is SurveyQuestion.Numeric -> !q.required || q.get(draft).toDoubleOrNull() != null || q.unknownGet(draft)
    is SurveyQuestion.NumericPair -> !q.required ||
        (q.leftGet(draft).toDoubleOrNull() != null && q.rightGet(draft).toDoubleOrNull() != null)
    is SurveyQuestion.Rating -> true
    is SurveyQuestion.Ranked -> draft.goalRanks.isNotEmpty()
    is SurveyQuestion.Identity -> draft.s0Valid
}

// "S2 · 질환·병력 · 단계 3/8" style eyebrow.
fun sectionEyebrow(current: SurveyQuestion): String {
    val (idx, _) = segmentInfo(current)
    return "단계 ${idx + 1}/8 · ${sectionTitle(current.section)}"
}

fun sectionTitle(s: SurveySection): String = when (s) {
    SurveySection.S0 -> "기본 인적사항"; SurveySection.S1 -> "신체 정보"
    SurveySection.S2 -> "질환·병력"; SurveySection.S3 -> "체감 증상"
    SurveySection.S4 -> "생활습관"; SurveySection.S5 -> "복용 중"
    SurveySection.S6 -> "관심 영역"; SurveySection.S7 -> "예산"
}
```

- [ ] **Step 4: Run tests — expect PASS.**

Run: `./gradlew :composeApp:allTests --rerun-tasks --console=plain 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, all green.

- [ ] **Step 5: Commit.**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyQuestions.kt \
        composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyQuestionsTest.kt
git commit -m "feat: pure survey nav/progress/answered helpers + tests"
```

---

## Task 4: Generic QuestionScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/QuestionScreen.kt`

This task is Compose UI (not unit-tested) — verified by compile + on-device QA in Task 7.

- [ ] **Step 1: Implement `QuestionScreen.kt`.**

Build a single `@Composable fun QuestionScreen(question, draft, onNext, onBack, onSkip)` that `when`-dispatches on the question type. Reuse the visual idiom from the current `SurveyFlow.kt` (CardShape = RoundedCornerShape(20.dp), ReverseGold/ReverseEspresso, FilterChip, FieldCard pattern). Key requirements (reproduce as actual code in the file):

- Top: a **segmented progress bar** — a Row of 8 segments; each segment is a thin rounded track; the active one fills to `segmentInfo(question).second` via `animateFloatAsState(animationSpec = tween(ANIM_MS))`; completed segments are full ReverseGold, future ones are `ReverseGold.copy(alpha = 0.12f)`.
- `sectionEyebrow(question)` text (labelMedium, ReverseGold) — hidden for Info screens.
- Big `prompt` (headlineSmall, ReverseEspresso, Bold); optional `helper` (onSurfaceVariant).
- Answer area dispatched by type:
  - **SingleChoice:** a column of large answer cards (full-width Card, 56.dp min height, CardShape). Selected = ReverseGold container + white text + trailing Check icon + a spring scale-pop (`animateFloatAsState` on a `targetScale`, `spring(dampingRatio = 0.45f)`). On tap: set the value, then `LaunchedEffect`-delay `AUTO_ADVANCE_MS` and call `onNext()`.
  - **MultiChoice:** the same cards but multi-toggle (write into `q.selected(draft)`), NO auto-advance. If `q.conditional` is set and its chip is selected, show an OutlinedTextField bound to the conditional get/set. SPECIAL CASE for id == "s2.immune": ALSO show the environmental-allergy field when `ImmuneAllergyCondition.EnvironmentalAllergy in draft.immuneAllergy`, bound to `draft.environmentalAllergyText`.
  - **Numeric:** an OutlinedTextField (number keyboard, digitsOnly), with the `unit` as suffix label; if `unknownable`, a "모름" FilterChip that toggles `unknownGet/unknownSet`.
  - **NumericPair:** two number fields in a Row (weight(1f) each).
  - **Rating:** the 1..5 row copied from the current `RatingRow` (enlarged to 36.dp height).
  - **Ranked:** the tap-to-rank goal list copied from the current `SectionGoals` (PriorityGoal.entries, draft.goalRanks, draft.toggleGoal).
  - **Identity (id "s0.identity"):** name OutlinedTextField + 출생연도/월/일 three number fields + 성별 SingleSelectChips(Gender) + 거주 지역 chips(Region) — copied from the current `SectionBasicInfo`.
  - **Info:** dispatch on `kind`:
    - **Notice:** the secondary-tinted card with Alert icon + body (copied from current `SensitiveNotice`). SPECIAL CASE id == "s5.prescription": render the 있음/없음 FilterChips + conditional 처방약 종류 OutlinedTextField (copied from current `SectionSupplements` prescription block) instead of a plain notice.
    - **Checkpoint:** centered column — a filling ring/badge (IconTile with LuminineIcon.Check or Sparkles, ReverseGold) with a scale-in entrance (`animateFloatAsState`), the `body` message (titleMedium), one CTA. Calm checkpoints (empty `prompt`) omit the celebratory icon emphasis.
    - **Reward:** the trophy finale (copied from current `RewardScreen`, IconTile LuminineIcon.Trophy) with a scale-in entrance + full segmented bar.
- Footer: a full-width primary Button (`ctaLabel` or "계속"), `enabled = isAnswered(question, draft)` for required types; below it, "이전" TextButton (if `onBack != null`) and "나중에 입력" TextButton (if `question.skippable`).
- Animation constants at top of file:

```kotlin
private const val ANIM_MS = 300            // progress/fill transitions
private const val AUTO_ADVANCE_MS = 250L   // single-choice snap delay
```

- [ ] **Step 2: Compile metadata + Android.**

Run: `./gradlew :composeApp:compileKotlinMetadata :composeApp:compileAndroidMain --console=plain 2>&1 | grep -E "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit.**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/QuestionScreen.kt
git commit -m "feat: generic QuestionScreen rendering any survey question type"
```

---

## Task 5: Rewrite SurveyFlow as a thin driver

**Files:**
- Rewrite: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyFlow.kt`

- [ ] **Step 1: Replace `SurveyFlow.kt` with the driver.**

```kotlin
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
 * One-question-per-screen onboarding survey. The flow is the data-driven [surveyQuestions] registry;
 * [QuestionScreen] renders the current question; this driver owns the draft + current-index state and
 * the next/back/skip/auto-advance transitions. onComplete receives the canonical SurveyResponse.
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
        current.section.let { draft.markSkipped(it) }
        goNext()
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
```

NOTE on skip semantics: in the old flow, tapping "나중에 입력" on a section marked the WHOLE section skipped and jumped to the next section. Here each section spans multiple questions. Skipping any question in a skippable section marks that section skipped (`draft.markSkipped`) — but the user lands on the next QUESTION, which may be in the same section. To match the old "skip the rest of this section" intent, `skip()` should advance to the first question whose section differs from `current.section`. Replace `skip()` with:

```kotlin
    fun skip() {
        draft.markSkipped(current.section)
        // Jump past the rest of this section (and its checkpoint) to the next section's first question.
        val nextDiff = surveyQuestions.drop(index + 1).firstOrNull { it.section != current.section }
        if (nextDiff == null) onComplete(draft.toResponse()) else index = surveyQuestions.indexOf(nextDiff)
    }
```

- [ ] **Step 2: Compile.**

Run: `./gradlew :composeApp:compileKotlinMetadata :composeApp:compileAndroidMain --console=plain 2>&1 | grep -E "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run full test suite (driver change must not break mapping tests).**

Run: `./gradlew :composeApp:allTests --rerun-tasks --console=plain 2>&1 | grep -E "FAILED|tests completed|BUILD"`
Expected: `BUILD SUCCESSFUL`, all 21 mapping tests + question/nav tests green.

- [ ] **Step 4: Commit.**

```bash
git add composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyFlow.kt
git commit -m "refactor: SurveyFlow becomes a thin driver over the question registry"
```

---

## Task 6: Remove dead SurveyStep flow code + tests

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyStep.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyStepTest.kt`

- [ ] **Step 1: Confirm what's now dead.**

Run: `cd /Users/hanshin/Documents/Projects/luminine && grep -rn --include="*.kt" "SurveyStep\b\|surveyFlowOrder\|countedSteps\|nextStep\|previousStep\|stepLabel" composeApp/src/commonMain`
Expected: NO matches in commonMain (the only user, old `SurveyFlow.kt`, was rewritten). If any match remains, stop and fix the reference before deleting.

- [ ] **Step 2: Delete `SurveyStep.kt` entirely.**

`rankedGoals` already moved to `SurveyQuestions.kt` (Task 1 Step 3). Everything else in `SurveyStep.kt` is the now-dead flow enum + helpers. Delete the file:

```bash
git rm composeApp/src/commonMain/kotlin/com/luminine/app/onboarding/SurveyStep.kt
```

- [ ] **Step 3: Delete the now-orphaned `SurveyStepTest.kt`.**

Its `rankedGoals` tests were re-created in `SurveyQuestionsTest.kt` (Task 2); the rest tested the deleted flow helpers.

```bash
git rm composeApp/src/commonTest/kotlin/com/luminine/app/onboarding/SurveyStepTest.kt
```

- [ ] **Step 4: Compile + full tests.**

Run: `./gradlew :composeApp:compileKotlinMetadata :composeApp:allTests --rerun-tasks --console=plain 2>&1 | grep -E "FAILED|tests completed|BUILD|error:"`
Expected: `BUILD SUCCESSFUL`, all green. If `compileKotlinMetadata` errors on an unresolved `SurveyStep`/helper reference, a usage was missed — restore from git and fix it.

- [ ] **Step 5: Commit.**

```bash
git add -A
git commit -m "chore: remove dead SurveyStep flow enum/helpers + their tests"
```

---

## Task 7: On-device QA + iOS sim compile

**Files:** none (verification only).

- [ ] **Step 1: Build + install the debug APK on the running emulator.**

```bash
cd /Users/hanshin/Documents/Projects/luminine
./gradlew :androidApp:assembleDebug --console=plain 2>&1 | tail -3
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
"$ADB" install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
"$ADB" shell pm clear com.luminine.app
"$ADB" shell am start -W -n com.luminine.app/.MainActivity
```
Expected: `Status: ok`. (First cold launch may take ~13s on this emulator — wait, don't treat the first-frame jank as a crash.)

- [ ] **Step 2: Drive the flow and screenshot each new screen type.**

Tap "카카오로 시작하기", then advance through: S0 Identity screen → S1 NumericPair + SingleChoice screens → a section checkpoint → the Notice → an S2 MultiChoice group (toggle 식품 알레르기 to confirm the conditional free-text appears) → an S3 group → S4 SingleChoice (confirm auto-advance) → S6 Ranked → reward. Capture screenshots to `/tmp/luminine-shots/gamified-*.png` and READ each one. Confirm: segmented progress bar fills, single-choice auto-advances, multi-choice needs "계속", checkpoints show, the conditional free-text appears, motion is subtle. Use `uiautomator dump` + computed chip bounds for taps (Korean `input text` fails on this emulator — use ASCII for the name field).

- [ ] **Step 3: iOS sim test target compiles.**

Run: `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64 --console=plain 2>&1 | grep -E "BUILD|error:"`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: No commit (verification only). If a bug is found, fix it in the relevant file with a TDD test where the logic is pure, then re-verify.**

---

## Task 8: Open PR to develop

- [ ] **Step 1: Push and open the PR.**

```bash
cd /Users/hanshin/Documents/Projects/luminine
git push -u luminine feature/onboarding-gamified
gh pr create --repo 203Projects/Luminine --base develop --head feature/onboarding-gamified \
  --title "feat: gamified one-question-per-screen onboarding survey" \
  --body "<summary of the calm-gamification design; link the spec; note it builds on PR #5 and keeps all 21 mapping tests green>"
```
Expected: PR URL printed. Note: if PR #5 is not yet merged, the diff will appear large because develop lacks the full-survey work; mention in the PR body that it stacks on PR #5.

- [ ] **Step 2: Wait for CI green; report the PR link + check status.**

---

## Self-Review notes (completed by plan author)

- **Spec coverage:** one-question-per-screen (Tasks 2,4,5) ✓; data-driven registry (Tasks 1-3) ✓; segmented progress (Task 4) ✓; auto-advance (Task 4) ✓; selection feedback/scale-pop (Task 4) ✓; section checkpoints content-aware (Task 2 `checkpoint(calm=)`, Task 4 Info/Checkpoint) ✓; finale (Task 4 Info/Reward) ✓; warm micro-copy (Task 2 prompts) ✓; tone guardrails — no points/streaks/mascot, calm on medical (Task 2 `calm=true` on s2.done) ✓; restrained motion constants, no new toggle (Task 4 ANIM_MS/AUTO_ADVANCE_MS) ✓; draft/model/mapping untouched, 21 tests green (Tasks 5,6 verify) ✓; SurveyStep dead-code removal w/ rankedGoals kept (Tasks 1,6) ✓; pure logic tested (Tasks 2,3) ✓; on-device QA (Task 7) ✓; PR to develop (Task 8) ✓.
- **Open implementation decision flagged for executor:** the registry models 처방약 (s5.prescription) and the S0 identity screen as special-cased renders rather than pure generic types — this is deliberate (they're genuinely bespoke) and documented at both the registry entry and the QuestionScreen dispatch. The `Identity` type was added to keep S0 first-class rather than hacked.
- **Type consistency:** `segmentInfo` returns `Pair<Int,Float>` (used in Task 3 test + Task 4 progress bar); `isAnswered`/`nextQuestion`/`prevQuestion`/`progressFraction`/`sectionEyebrow` signatures match between definition (Task 3) and use (Task 4/5). `checkpoint(...)` factory signature matches its calls in Task 2.
