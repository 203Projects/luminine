package com.luminine.app.domain

import com.luminine.app.model.Condition
import com.luminine.app.model.DailyRecord
import com.luminine.app.model.MemberDailySummary
import com.luminine.app.model.Routine
import com.luminine.app.model.RoutineCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class RoutineCalculationsTest {
    @Test
    fun practiceScoreUsesOnlyActiveRoutinesAndRoundsToNearestPoint() {
        val routines = listOf(
            Routine("r1", "u1", "비타민C 복용", RoutineCategory.InnerCare, isActive = true, order = 0),
            Routine("r2", "u1", "20분 걷기", RoutineCategory.Exercise, isActive = true, order = 1),
            Routine("r3", "u1", "저녁 스킨케어", RoutineCategory.SkinCare, isActive = true, order = 2),
            Routine("r4", "u1", "비활성 루틴", RoutineCategory.Sleep, isActive = false, order = 3),
        )

        val score = calculatePracticeScore(routines, doneRoutineIds = setOf("r1", "r3", "r4"))

        assertEquals(67, score)
    }

    @Test
    fun practiceScoreIsZeroWhenNoActiveRoutineExists() {
        val routines = listOf(
            Routine("r1", "u1", "비활성 루틴", RoutineCategory.InnerCare, isActive = false, order = 0),
        )

        assertEquals(0, calculatePracticeScore(routines, doneRoutineIds = setOf("r1")))
    }

    @Test
    fun attentionMembersIncludeLowEnergyOrLowPracticeScore() {
        val members = listOf(
            MemberDailySummary("u1", "민지", score = 86, energy = 5),
            MemberDailySummary("u2", "서연", score = 30, energy = 4),
            MemberDailySummary("u3", "지훈", score = 72, energy = 3),
        )

        val attention = membersRequiringAttention(members)

        assertEquals(listOf("u2", "u3"), attention.map { it.userId })
    }

    @Test
    fun monthlyCalendarBucketsRecordsIntoNonePartialAndComplete() {
        val records = listOf(
            DailyRecord(
                id = "d1",
                userId = "u1",
                date = LocalDate(2026, 5, 1),
                score = 0,
                doneRoutineIds = emptySet(),
                totalRoutines = 4,
                condition = Condition(energy = 2, skin = 3, sleep = 2, emoji = "😕"),
                memo = null,
            ),
            DailyRecord(
                id = "d2",
                userId = "u1",
                date = LocalDate(2026, 5, 2),
                score = 50,
                doneRoutineIds = setOf("r1", "r2"),
                totalRoutines = 4,
                condition = Condition(energy = 4, skin = 4, sleep = 4, emoji = "😊"),
                memo = null,
            ),
            DailyRecord(
                id = "d3",
                userId = "u1",
                date = LocalDate(2026, 5, 3),
                score = 100,
                doneRoutineIds = setOf("r1", "r2", "r3", "r4"),
                totalRoutines = 4,
                condition = Condition(energy = 5, skin = 5, sleep = 5, emoji = "🔥"),
                memo = null,
            ),
        )

        val buckets = monthlyCompletionBuckets(records)

        assertEquals(CompletionBucket.None, buckets[LocalDate(2026, 5, 1)])
        assertEquals(CompletionBucket.Partial, buckets[LocalDate(2026, 5, 2)])
        assertEquals(CompletionBucket.Complete, buckets[LocalDate(2026, 5, 3)])
        assertTrue(LocalDate(2026, 5, 4) !in buckets)
    }
}
