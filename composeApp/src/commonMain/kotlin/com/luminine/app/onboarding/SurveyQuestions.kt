package com.luminine.app.onboarding

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

// Checkpoint factory. `calm = true` tones down the celebration for medical sections.
private fun checkpoint(id: String, section: SurveySection, message: String, calm: Boolean = false) =
    SurveyQuestion.Info(id = id, section = section, prompt = if (calm) "" else "잘하고 있어요",
        body = message, ctaLabel = "계속", kind = InfoKind.Checkpoint)

// The ordered one-question-per-screen flow. Each entry binds to a SurveyDraft field via lambdas;
// Info(Checkpoint) entries cap each section, Info(Notice) precedes S2, Info(Reward) closes the flow.
val surveyQuestions: List<SurveyQuestion> = buildList {
    // ---------- S0 기본 인적사항 (required) ----------
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
    // `conditional`. The second (environmental) is rendered inside QuestionScreen by also checking
    // ImmuneAllergyCondition.EnvironmentalAllergy directly. Handled by the later screen task.
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
    // 처방약 여부 (있음/없음 + conditional note) is a small bespoke screen — rendered as an Info-less
    // special case in QuestionScreen keyed by id "s5.prescription". Handled by the later screen task.
    add(SurveyQuestion.Info(id = "s5.prescription", section = S5, prompt = "처방약을 복용 중인가요?",
        body = "영양제 추천 충돌을 막기 위해 사용돼요.", ctaLabel = "계속", kind = InfoKind.Notice)) // rendered specially
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

// ============================================================
// PURE NAVIGATION / PROGRESS / ANSWERED HELPERS (unit-tested)
// ============================================================

fun nextQuestion(current: SurveyQuestion): SurveyQuestion? =
    surveyQuestions.getOrNull(surveyQuestions.indexOf(current) + 1)

fun prevQuestion(current: SurveyQuestion): SurveyQuestion? =
    surveyQuestions.getOrNull(surveyQuestions.indexOf(current) - 1)

// Monotonic 0f..1f over the whole flow, by position. Reward (last) == 1f.
fun progressFraction(current: SurveyQuestion): Float {
    val i = surveyQuestions.indexOf(current)
    return (i + 1).toFloat() / surveyQuestions.size
}

private val sectionOrder = SurveySection.entries // S0..S7 in declaration order

// (sectionIndex 0..7, fraction-through-that-section 0f..1f) — drives the segmented progress bar.
fun segmentInfo(current: SurveyQuestion): Pair<Int, Float> {
    val idx = sectionOrder.indexOf(current.section)
    val inSection = surveyQuestions.filter { it.section == current.section }
    val pos = inSection.indexOf(current)
    val frac = (pos + 1).toFloat() / inSection.size
    return idx to frac
}

// Whether a question's required input is satisfied — gates the "계속" button. Multi-select and
// rating are always "answerable" (they may legitimately be empty); Info screens are always answered.
fun isAnswered(q: SurveyQuestion, draft: SurveyDraft): Boolean = when (q) {
    is SurveyQuestion.Info -> true
    is SurveyQuestion.SingleChoice<*> -> !q.required || q.get(draft) != null
    is SurveyQuestion.MultiChoice<*> -> true
    is SurveyQuestion.Numeric -> !q.required || q.get(draft).toDoubleOrNull() != null || q.unknownGet(draft)
    is SurveyQuestion.NumericPair -> !q.required ||
        (q.leftGet(draft).toDoubleOrNull() != null && q.rightGet(draft).toDoubleOrNull() != null)
    is SurveyQuestion.Rating -> true
    is SurveyQuestion.Ranked -> draft.goalRanks.isNotEmpty()
    is SurveyQuestion.Identity -> draft.s0Valid
}

// "단계 3/8 · 질환·병력" eyebrow shown above the prompt (hidden for Info screens by the UI).
fun sectionEyebrow(current: SurveyQuestion): String {
    val (idx, _) = segmentInfo(current)
    return "단계 ${idx + 1}/8 · ${sectionTitle(current.section)}"
}

fun sectionTitle(s: SurveySection): String = when (s) {
    SurveySection.S0 -> "기본 인적사항"
    SurveySection.S1 -> "신체 정보"
    SurveySection.S2 -> "질환·병력"
    SurveySection.S3 -> "체감 증상"
    SurveySection.S4 -> "생활습관"
    SurveySection.S5 -> "복용 중"
    SurveySection.S6 -> "관심 영역"
    SurveySection.S7 -> "예산"
}
