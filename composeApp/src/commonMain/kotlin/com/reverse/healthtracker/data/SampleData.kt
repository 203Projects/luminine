package com.reverse.healthtracker.data

import com.reverse.healthtracker.model.AiAdvice
import com.reverse.healthtracker.model.AdviceTrigger
import com.reverse.healthtracker.model.Condition
import com.reverse.healthtracker.model.DailyRecord
import com.reverse.healthtracker.model.InbodyRecord
import com.reverse.healthtracker.model.KakaoMessage
import com.reverse.healthtracker.model.MemberDailySummary
import com.reverse.healthtracker.model.RecordSource
import com.reverse.healthtracker.model.Routine
import com.reverse.healthtracker.model.RoutineCategory
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

object SampleData {
    private val now = LocalDateTime(2026, 5, 20, 9, 30)
    val today: LocalDate = now.date

    fun defaultRoutines(userId: String = "user-1"): List<Routine> = listOf(
        Routine("r-inner-1", userId, "비타민C 이너케어 복용", RoutineCategory.InnerCare, true, 0),
        Routine("r-exercise-1", userId, "20분 걷기 또는 근력운동", RoutineCategory.Exercise, true, 1),
        Routine("r-diet-1", userId, "단백질 중심 식사 기록", RoutineCategory.Diet, true, 2),
        Routine("r-skin-1", userId, "저녁 스킨케어 루틴", RoutineCategory.SkinCare, true, 3),
        Routine("r-sleep-1", userId, "자정 전 수면 준비", RoutineCategory.Sleep, true, 4),
        Routine("r-mind-1", userId, "5분 호흡 또는 감사 기록", RoutineCategory.Mind, true, 5),
    )

    fun records(userId: String = "user-1"): List<DailyRecord> = listOf(
        DailyRecord("d-1", userId, LocalDate(2026, 5, 14), 83, setOf("r-inner-1", "r-exercise-1", "r-diet-1", "r-skin-1", "r-sleep-1"), 6, Condition(4, 4, 3, "😊"), "저녁 식사 가볍게 유지"),
        DailyRecord("d-2", userId, LocalDate(2026, 5, 15), 67, setOf("r-inner-1", "r-diet-1", "r-skin-1", "r-sleep-1"), 6, Condition(3, 4, 3, "😕"), null),
        DailyRecord("d-3", userId, LocalDate(2026, 5, 16), 100, setOf("r-inner-1", "r-exercise-1", "r-diet-1", "r-skin-1", "r-sleep-1", "r-mind-1"), 6, Condition(5, 5, 4, "🔥"), "컨디션 좋음"),
        DailyRecord("d-4", userId, LocalDate(2026, 5, 17), 50, setOf("r-inner-1", "r-skin-1", "r-sleep-1"), 6, Condition(3, 3, 2, "😕"), "수면 부족"),
        DailyRecord("d-5", userId, LocalDate(2026, 5, 18), 83, setOf("r-inner-1", "r-exercise-1", "r-diet-1", "r-skin-1", "r-mind-1"), 6, Condition(4, 4, 4, "😊"), null),
        DailyRecord("d-6", userId, LocalDate(2026, 5, 19), 67, setOf("r-inner-1", "r-diet-1", "r-skin-1", "r-sleep-1"), 6, Condition(4, 3, 4, "😄"), null),
        DailyRecord("d-7", userId, today, 0, emptySet(), 6, Condition(3, 3, 3, "😊"), null),
    )

    fun latestInbody(userId: String = "user-1"): InbodyRecord =
        InbodyRecord(
            id = "inbody-1",
            userId = userId,
            measuredAt = now,
            weight = 62.4,
            bodyFatPct = 22.1,
            muscleMass = 24.8,
            bmr = 1320,
            bodyWater = 32.4,
            visceralFat = 6,
            source = RecordSource.Kakao,
            rawMessage = "오늘 인바디 62.4kg 체지방 22.1% 골격근 24.8kg",
        )

    fun aiAdvice(userId: String = "user-1"): AiAdvice =
        AiAdvice(
            id = "advice-1",
            userId = userId,
            triggeredBy = AdviceTrigger.KakaoMessage,
            inputSummary = "인바디, 식단, 영양제, 실천지수 83점",
            adviceText = "오늘은 단백질 식사와 이너케어가 안정적으로 이어졌어요. 체지방률 22.1% 구간에서는 저녁 탄수화물을 조금만 줄이고, 내일은 식후 15분 걷기를 추가해 산화 스트레스 관리를 이어가세요.",
            createdAt = now,
        )

    fun memberSummaries(): List<MemberDailySummary> = listOf(
        MemberDailySummary("u-1", "김민지", 83, 4),
        MemberDailySummary("u-2", "이서연", 30, 4),
        MemberDailySummary("u-3", "박지훈", 72, 3),
        MemberDailySummary("u-4", "최하린", 100, 5),
    )

    fun kakaoMessages(): List<KakaoMessage> = listOf(
        KakaoMessage("k-1", "u-1", "인바디 62.4kg 체지방 22.1%", "inbody", "체중 62.4kg, 체지방 22.1%", now, "advice-1"),
        KakaoMessage("k-2", "u-2", "점심 현미밥+닭가슴살", "diet", "점심 식단 기록", now, "advice-2"),
        KakaoMessage("k-3", "u-3", "비타민C 3g, 오메가3 복용", "supplement", "영양제 2종", now, "advice-3"),
    )
}
