package com.luminine.app.domain

import com.luminine.app.model.DailyRecord
import com.luminine.app.model.MemberDailySummary
import com.luminine.app.model.Routine
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

enum class CompletionBucket {
    None,
    Partial,
    Complete,
}

fun calculatePracticeScore(
    routines: List<Routine>,
    doneRoutineIds: Set<String>,
): Int {
    val activeRoutineIds = routines.filter { it.isActive }.map { it.id }.toSet()
    if (activeRoutineIds.isEmpty()) return 0

    val doneActiveCount = doneRoutineIds.count { it in activeRoutineIds }
    return ((doneActiveCount.toDouble() / activeRoutineIds.size) * 100).roundToInt()
}

fun membersRequiringAttention(members: List<MemberDailySummary>): List<MemberDailySummary> =
    members.filter { it.energy <= 3 || it.score <= 30 }

fun monthlyCompletionBuckets(records: List<DailyRecord>): Map<LocalDate, CompletionBucket> =
    records.associate { record ->
        val bucket = when (record.score) {
            0 -> CompletionBucket.None
            100 -> CompletionBucket.Complete
            else -> CompletionBucket.Partial
        }
        record.date to bucket
    }

fun averageScore(records: List<DailyRecord>): Int =
    records.map { it.score }.averageOrZero().roundToInt()

fun averageEnergy(records: List<DailyRecord>): Double =
    records.map { it.condition.energy }.averageOrZero()

private fun List<Int>.averageOrZero(): Double =
    if (isEmpty()) 0.0 else average()
