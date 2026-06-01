package com.luminine.app.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

data class User(
    val id: String,
    val kakaoId: String,
    val name: String,
    val nickname: String?,
    val phone: String?,
    val joinedAt: LocalDateTime,
    val profileImage: String?,
    val totalPoints: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val routines: List<Routine>,
)

data class Routine(
    val id: String,
    val userId: String,
    val name: String,
    val category: RoutineCategory,
    val isActive: Boolean,
    val order: Int,
)

enum class RoutineCategory(val label: String) {
    InnerCare("이너케어"),
    Exercise("운동"),
    Diet("식단"),
    SkinCare("스킨케어"),
    Sleep("수면"),
    Mind("마음건강"),
}

data class DailyRecord(
    val id: String,
    val userId: String,
    val date: LocalDate,
    val score: Int,
    val doneRoutineIds: Set<String>,
    val totalRoutines: Int,
    val condition: Condition,
    val memo: String?,
)

data class Condition(
    val energy: Int,
    val skin: Int,
    val sleep: Int,
    val emoji: String,
)

data class InbodyRecord(
    val id: String,
    val userId: String,
    val measuredAt: LocalDateTime,
    val weight: Double,
    val bodyFatPct: Double,
    val muscleMass: Double,
    val bmr: Int?,
    val bodyWater: Double?,
    val visceralFat: Int?,
    val source: RecordSource,
    val rawMessage: String?,
)

enum class RecordSource {
    Kakao,
    Manual,
}

data class DietRecord(
    val id: String,
    val userId: String,
    val date: LocalDate,
    val mealType: String,
    val content: String,
    val imageUrl: String?,
    val source: RecordSource,
    val rawMessage: String?,
)

data class SupplementRecord(
    val id: String,
    val userId: String,
    val date: LocalDate,
    val items: List<String>,
    val source: RecordSource,
    val rawMessage: String?,
)

data class AiAdvice(
    val id: String,
    val userId: String,
    val triggeredBy: AdviceTrigger,
    val inputSummary: String,
    val adviceText: String,
    val createdAt: LocalDateTime,
)

enum class AdviceTrigger {
    KakaoMessage,
    DailyRecord,
    Manual,
}

data class KakaoMessage(
    val id: String,
    val userId: String,
    val rawText: String,
    val parsedType: String,
    val parsedDataSummary: String,
    val processedAt: LocalDateTime,
    val aiAdviceId: String?,
)

data class MemberDailySummary(
    val userId: String,
    val name: String,
    val score: Int,
    val energy: Int,
)
