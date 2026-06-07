package com.luminine.app.model

import kotlinx.serialization.Serializable

// ============================================================
// Onboarding survey domain model — every field from
// /Users/hanshin/Downloads/LUMININE_온보딩_설문_구성.md (v1.0).
// Idiom matches existing models: enum class X(val label: String).
// Korean labels are the verbatim option text shown to the user.
// All types are @Serializable so SurveyResponse persists as JSON.
// ============================================================

// --- S0 기본 인적 사항 ---
enum class Gender(val label: String) { Male("남"), Female("여"), Other("기타") }

// 거주 지역 — 시/도 (17 administrative regions; label shown in the picker).
enum class Region(val label: String) {
    Seoul("서울"), Busan("부산"), Daegu("대구"), Incheon("인천"), Gwangju("광주"),
    Daejeon("대전"), Ulsan("울산"), Sejong("세종"), Gyeonggi("경기"), Gangwon("강원"),
    Chungbuk("충북"), Chungnam("충남"), Jeonbuk("전북"), Jeonnam("전남"),
    Gyeongbuk("경북"), Gyeongnam("경남"), Jeju("제주"),
}

// --- S1 신체 기본 정보 (categorical fields; numeric fields live on the data class) ---
enum class BloodPressureStatus(val label: String) { Normal("정상"), High("고혈압"), Low("저혈압"), Unknown("모름") }
enum class BloodSugarStatus(val label: String) { Normal("정상"), HighFasting("공복혈당 이상"), Diabetes("당뇨"), Unknown("모름") }
enum class VisionStatus(val label: String) { Normal("정상"), GlassesOrLens("안경·렌즈 착용"), LasikLasek("라식·라섹") }
enum class HearingStatus(val label: String) { Normal("정상"), Impaired("이상 있음") }

// --- S2 질환·병력 — grouped multi-select (one enum per group, stored as Set) ---
enum class CardioMetabolicCondition(val label: String) {
    Hypertension("고혈압"), Hypotension("저혈압"), Hyperlipidemia("고지혈증"),
    DiabetesType1("당뇨 (1형)"), DiabetesType2("당뇨 (2형)"), MetabolicSyndrome("대사증후군"),
    HeartDisease("심장질환 (부정맥, 협심증 등)"),
}
enum class DigestiveCondition(val label: String) {
    Reflux("역류성 식도염"), Ibs("과민성대장증후군 (IBS)"), Ulcer("위궤양 / 십이지장궤양"),
    LiverDisease("간질환 (지방간, 간염 등)"), CrohnsColitis("크론병 / 궤양성대장염"),
}
enum class MusculoskeletalCondition(val label: String) {
    CervicalDisc("경추 디스크"), LumbarDisc("요추 디스크"), KneeArthritis("무릎 관절염"),
    HipDisease("고관절 질환"), ShoulderDisease("어깨 질환 (회전근개 등)"),
    Osteoporosis("골다공증"), Scoliosis("척추측만증"),
}
enum class HormoneCondition(val label: String) {
    Hyperthyroidism("갑상선 기능 항진증"), Hypothyroidism("갑상선 기능 저하증"),
    AdrenalFatigue("부신 피로"), FemaleHormoneDisorder("여성호르몬 이상 (PCOS 등)"),
    MaleHormoneLow("남성호르몬 저하 / 남성 갱년기"),
}
enum class NeuroPsychCondition(val label: String) {
    Insomnia("수면장애 (불면증)"), Depression("우울증"), Anxiety("불안장애"),
    ChronicHeadache("만성두통 / 편두통"), Burnout("번아웃 / 만성피로증후군"),
}
enum class ImmuneAllergyCondition(val label: String) {
    AtopicDermatitis("아토피 피부염"), Autoimmune("자가면역질환 (류마티스 등)"),
    FoodAllergy("식품 알레르기"), EnvironmentalAllergy("환경성 알레르기"),
}
enum class OtherCondition(val label: String) {
    KidneyDisease("신장질환"), CancerHistory("암 병력"), None("해당 없음"),
}

// --- S3 걱정되는 건강 문제 (체감 증상) — grouped multi-select ---
enum class EnergySymptom(val label: String) {
    ChronicFatigue("만성피로 (항상 피곤함)"), AfternoonFocusDrop("오후 집중력 저하"),
    MorningFatigue("기상 후에도 피로감 지속"), SlowRecovery("운동 후 회복이 더딤"),
}
enum class BodyShapeSymptom(val label: String) {
    AbdominalFat("복부 지방 증가"), MuscleLoss("전반적인 근육량 감소"),
    HardToLoseWeight("체중 감량이 잘 안 됨"), Edema("부종 (얼굴·다리)"),
}
enum class SkinSymptom(val label: String) {
    ElasticityLoss("피부 탄력 저하"), Wrinkles("주름 증가"),
    Pigmentation("기미·잡티·색소침착"), HairLoss("탈모 / 모발 약화"),
    BrittleNails("손발톱 약함·갈라짐"),
}
enum class DigestiveSymptom(val label: String) {
    Indigestion("소화불량 / 더부룩함"), Constipation("변비"),
    Bloating("복부팽만감"), Diarrhea("잦은 설사 / 묽은 변"),
}
enum class SleepSymptom(val label: String) {
    HardToFallAsleep("잠들기 어려움 (입면 장애)"), FrequentWaking("자다가 자주 깸"),
    Unrefreshing("자고 일어나도 개운하지 않음"), Hypersomnia("과수면 / 낮에 졸림"),
}
enum class CognitiveSymptom(val label: String) {
    MemoryDecline("기억력 저하"), FocusLoss("집중력 감소 / 멍함"),
    MoodSwings("감정 기복 심함"), Lethargy("무기력감 / 의욕 저하"),
}
enum class HormonalSymptom(val label: String) {
    LowLibido("성욕 감소"), Menopause("갱년기 증상 (열감, 식은땀)"),
    MenstrualIssues("생리 불순 / 생리통 심화 (여성)"), ErectileDysfunction("발기부전 / 성기능 저하 (남성)"),
}
enum class JointPainSymptom(val label: String) {
    KneePain("무릎 통증"), BackPain("허리 통증"),
    ShoulderNeckPain("어깨·목 통증"), NumbnessCold("손발 저림 / 냉감"),
}

// --- S4 생활 습관 ---
enum class MealCount(val label: String) { One("1끼"), Two("2끼"), Three("3끼"), FourPlus("4끼 이상") }
enum class MealRegularity(val label: String) { Regular("규칙적"), Irregular("불규칙") }
enum class StapleDietType(val label: String) {
    Korean("한식 중심"), WesternMixed("양식 혼합"), VegetarianOriented("채식 지향"), HighProtein("고단백 식단 실천 중"),
}
enum class DietRestriction(val label: String) {
    GlutenFree("글루텐 프리"), DairyFree("유제품 제한"),
    Vegetarian("채식(베지테리언)"), Vegan("비건"), None("없음"),
}
enum class WaterIntake(val label: String) { UnderOneLiter("1L 미만"), OneToOneHalf("1~1.5L"), OneHalfToTwo("1.5~2L"), TwoPlus("2L 이상") }
enum class AlcoholFrequency(val label: String) { Rarely("거의 안 함"), MonthlyOneTwo("월 1~2회"), WeeklyOneTwo("주 1~2회"), WeeklyThreePlus("주 3회 이상") }
enum class SmokingStatus(val label: String) { NonSmoker("비흡연"), Smoker("흡연"), Vaping("전자담배"), Quitting("금연 중") }
enum class CaffeineIntake(val label: String) { None("없음"), OneCup("하루 1잔"), TwoThree("2~3잔"), FourPlus("4잔 이상") }

enum class ExerciseFrequency(val label: String) {
    Never("전혀 안 함"), Rarely("가끔(월 1~2회)"), WeeklyOneTwo("주 1~2회"),
    WeeklyThreeFour("주 3~4회"), AlmostDaily("거의 매일"),
}
enum class ExerciseType(val label: String) {
    WalkingJogging("걷기·조깅"), Strength("근력운동"), BallSports("구기종목"),
    Swimming("수영"), YogaPilates("요가·필라테스"), Mixed("복합"),
}
enum class ExerciseIntensity(val label: String) { Light("가벼움"), Moderate("보통"), High("고강도") }
enum class ExerciseDuration(val label: String) { Under30("30분 미만"), ThirtyToSixty("30~60분"), SixtyToNinety("60~90분"), NinetyPlus("90분 이상") }
enum class ExerciseGoal(val label: String) {
    WeightLoss("체중 감량"), MuscleGain("근육 증가"), Fitness("체력 유지"),
    RehabPain("재활·통증 관리"), StressRelief("스트레스 해소"),
}

enum class SleepDuration(val label: String) {
    Under5("5시간 미만"), FiveToSix("5~6시간"), SixToSeven("6~7시간"),
    SevenToEight("7~8시간"), EightPlus("8시간 이상"),
}
enum class BedtimeRange(val label: String) { Before22("22시 이전"), TwentyTwoToMidnight("22~24시"), AfterMidnight("자정 이후") }
enum class SleepAid(val label: String) { None("없음"), Melatonin("멜라토닌"), Prescription("수면제(처방)"), Other("기타") }

enum class StressSource(val label: String) {
    Work("직장·업무"), Relationships("인간관계"), Financial("경제적"),
    HealthWorry("건강 걱정"), Other("기타"),
}
enum class RelaxationActivity(val label: String) {
    Meditation("명상·호흡법"), Hobby("취미활동"), ExerciseRelief("운동으로 해소"), None("없음"),
}

// --- S5 현재 복용 중인 것 ---
enum class Supplement(val label: String) {
    Multivitamin("종합비타민"), VitaminC("비타민 C"), VitaminD("비타민 D"),
    Omega3("오메가3 (EPA/DHA)"), Magnesium("마그네슘"), Zinc("아연"), Iron("철분"),
    Collagen("콜라겐"), Probiotics("프로바이오틱스 (유산균)"), Protein("단백질 보충제 (프로틴)"),
    Glutathione("글루타치온"), CoQ10("CoQ10 (코엔자임 Q10)"), Lutein("루테인 / 지아잔틴"),
    Calcium("칼슘"), NadNmn("NAD+ / NMN"), None("없음"),
}
enum class AllergenComponent(val label: String) {
    None("없음"), Shellfish("갑각류 (새우, 게 등)"), Nuts("견과류"),
    Dairy("유제품 (유당불내증 포함)"), Gluten("글루텐 (밀)"),
}

// --- S6 안티에이징 관심 영역 및 목표 — 10 goals, ranked (1순위 = index 0) ---
enum class PriorityGoal(val label: String) {
    WeightBody("체중·체형 관리"),
    SkinAging("피부 노화 개선 (탄력, 주름, 광채)"),
    EnergyVitality("에너지·활력 회복"),
    MuscleMaintain("근육량 유지·증가"),
    SleepQuality("수면 질 개선"),
    Cognitive("인지기능·집중력·기억력"),
    GutHealth("장 건강·소화 개선"),
    HormoneBalance("호르몬 밸런스 (갱년기 포함)"),
    Immunity("면역력 강화"),
    JointPain("관절·통증 관리"),
}

// --- S7 라이프스타일 & 예산 (선택) ---
enum class JobType(val label: String) {
    OfficeSitting("사무직(주로 앉음)"), FieldStanding("현장직(주로 서거나 움직임)"),
    Remote("재택근무"), Other("기타"),
}
enum class WalkingTime(val label: String) { Under30("30분 미만"), ThirtyToOneHour("30분~1시간"), OneToTwo("1~2시간"), TwoPlus("2시간 이상") }
enum class MonthlyBudget(val label: String) { Under100k("10만원 미만"), From100kTo300k("10~30만원"), From300kTo500k("30~50만원"), Over500k("50만원 이상") }
enum class ConsultingInterest(val label: String) { Interested("관심 있음"), Later("나중에"), NotNeeded("필요 없음") }

// ============================================================
// SECTION DATA CLASSES
// ============================================================

// S0 — REQUIRED. Fields individually nullable so a partially-typed form persists
// section-by-section; validation enforces the non-null requirement. birthdate is stored as
// Int fields (not kotlinx.datetime.LocalDate) so the JSON is self-contained — no contextual
// datetime serializer needed on any KMP target.
// MVP UI note: onboarding collects birthYear only (sufficient for an age band); birthMonth/birthDay
// are kept on the model for a future full 날짜 선택 picker (the doc's "나이 자동 계산").
@Serializable
data class BasicInfoSection(
    val name: String = "",
    val birthYear: Int? = null,
    val birthMonth: Int? = null,
    val birthDay: Int? = null,
    val gender: Gender? = null,
    val region: Region? = null,
)

// S1 — REQUIRED (height + weight). Other numeric fields nullable; null == "모름" / not entered.
@Serializable
data class BodyInfoSection(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,   // null = 모름
    val muscleMassKg: Double? = null, // null = 모름
    val waistCm: Double? = null,      // null = 모름
    val bloodPressure: BloodPressureStatus? = null,
    val bloodSugar: BloodSugarStatus? = null,
    val vision: VisionStatus? = null,
    val hearing: HearingStatus? = null,
)

// S2 — SKIPPABLE. Multi-select sets + free text. Whole section nullable on SurveyResponse.
@Serializable
data class ConditionsSection(
    val cardioMetabolic: Set<CardioMetabolicCondition> = emptySet(),
    val digestive: Set<DigestiveCondition> = emptySet(),
    val musculoskeletal: Set<MusculoskeletalCondition> = emptySet(),
    val hormone: Set<HormoneCondition> = emptySet(),
    val neuroPsych: Set<NeuroPsychCondition> = emptySet(),
    val immuneAllergy: Set<ImmuneAllergyCondition> = emptySet(),
    val other: Set<OtherCondition> = emptySet(),
    val foodAllergyText: String? = null,          // 식품 알레르기 직접 입력
    val environmentalAllergyText: String? = null, // 환경성 알레르기 직접 입력
    val otherConditionText: String? = null,       // 기타 직접 입력
)

// S3 — SKIPPABLE. Grouped symptom multi-select.
@Serializable
data class SymptomsSection(
    val energy: Set<EnergySymptom> = emptySet(),
    val bodyShape: Set<BodyShapeSymptom> = emptySet(),
    val skin: Set<SkinSymptom> = emptySet(),
    val digestive: Set<DigestiveSymptom> = emptySet(),
    val sleep: Set<SleepSymptom> = emptySet(),
    val cognitive: Set<CognitiveSymptom> = emptySet(),
    val hormonal: Set<HormonalSymptom> = emptySet(),
    val jointPain: Set<JointPainSymptom> = emptySet(),
)

// S4 — SKIPPABLE. Exercise sub-fields drive Home seeding. Sleep/stress are 1..5 self-ratings.
@Serializable
data class LifestyleSection(
    // 식사 패턴
    val mealCount: MealCount? = null,
    val mealRegularity: MealRegularity? = null,
    val stapleDietType: StapleDietType? = null,
    val dietRestrictions: Set<DietRestriction> = emptySet(),
    val waterIntake: WaterIntake? = null,
    val alcoholFrequency: AlcoholFrequency? = null,
    val smokingStatus: SmokingStatus? = null,
    val caffeineIntake: CaffeineIntake? = null,
    // 운동
    val exerciseFrequency: ExerciseFrequency? = null,
    val exerciseTypes: Set<ExerciseType> = emptySet(),
    val exerciseIntensity: ExerciseIntensity? = null,
    val exerciseDuration: ExerciseDuration? = null,
    val exerciseGoals: Set<ExerciseGoal> = emptySet(),
    // 수면
    val sleepDuration: SleepDuration? = null,
    val bedtime: BedtimeRange? = null,
    val sleepQuality: Int? = null,   // 1..5 자가평가
    val sleepAid: SleepAid? = null,
    val sleepAidOtherText: String? = null,
    // 스트레스 / 정신
    val stressLevel: Int? = null,    // 1..5
    val stressSources: Set<StressSource> = emptySet(),
    val relaxationActivities: Set<RelaxationActivity> = emptySet(),
)

// S5 — SKIPPABLE. 영양제 multi + 처방약 + 알레르기 성분.
@Serializable
data class SupplementsSection(
    val supplements: Set<Supplement> = emptySet(),
    val supplementOtherText: String? = null,
    val takingPrescription: Boolean? = null,  // null = 미입력, false = 없음, true = 있음
    val prescriptionText: String? = null,     // 처방약 종류 직접 입력
    val allergens: Set<AllergenComponent> = emptySet(),
    val allergenOtherText: String? = null,
)

// S6 — REQUIRED. Ordered 1..N (index 0 = 1순위).
@Serializable
data class GoalsSection(
    val orderedGoals: List<PriorityGoal> = emptyList(),
)

// S7 — SKIPPABLE (entire section optional per doc).
@Serializable
data class BudgetLifestyleSection(
    val jobType: JobType? = null,
    val walkingTime: WalkingTime? = null,
    val monthlyBudget: MonthlyBudget? = null,
    val consultingInterest: ConsultingInterest? = null,
)

// ============================================================
// AGGREGATE
// ============================================================

// Section key used in completedSections / skippedSections.
@Serializable
enum class SurveySection { S0, S1, S2, S3, S4, S5, S6, S7 }

// Top-level survey aggregate. Required sections (S0/S1/S6) are non-null with empty defaults so
// partial section-by-section save works; skippable sections (S2/S3/S4/S5/S7) are nullable, where
// null == "나중에 입력" (not yet provided). completedSections records explicit completion (a user may
// complete a skippable section with zero selections — still "completed", distinct from skipped).
// skippedSections records sections the user explicitly tapped "나중에 입력" (drives 마이페이지 prompts).
@Serializable
data class SurveyResponse(
    val basicInfo: BasicInfoSection = BasicInfoSection(),
    val bodyInfo: BodyInfoSection = BodyInfoSection(),
    val conditions: ConditionsSection? = null,
    val symptoms: SymptomsSection? = null,
    val lifestyle: LifestyleSection? = null,
    val supplements: SupplementsSection? = null,
    val goals: GoalsSection = GoalsSection(),
    val budgetLifestyle: BudgetLifestyleSection? = null,
    val completedSections: Set<SurveySection> = emptySet(),
    val skippedSections: Set<SurveySection> = emptySet(),
    val schemaVersion: Int = 1,
)
