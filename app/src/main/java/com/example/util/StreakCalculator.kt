package com.example.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure date/streak helpers for the habit tracker.
 * Epoch days are local-calendar days, so check-ins align with the user's day.
 */
object StreakCalculator {

    fun localEpochDay(
        timeMillis: Long = System.currentTimeMillis(),
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val offset = zone.getOffset(timeMillis)
        return Math.floorDiv(timeMillis + offset, 86_400_000L)
    }

    fun currentStreak(checkedDays: Set<Long>, today: Long): Int {
        var cursor = if (today in checkedDays) today else today - 1
        var streak = 0
        while (cursor in checkedDays) {
            streak++
            cursor--
        }
        return streak
    }

    fun lastDays(today: Long, count: Int = 7): List<Long> {
        if (count <= 0) return emptyList()
        return ((count - 1) downTo 0).map { today - it }
    }

    fun checksInWindow(checkedDays: Set<Long>, today: Long, windowDays: Int = 7): Int =
        lastDays(today, windowDays).count { it in checkedDays }
}
