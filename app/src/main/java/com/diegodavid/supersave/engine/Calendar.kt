package com.diegodavid.supersave.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A run of days, both ends included. (Calendar helper written by Claude, Nivel 2.) */
data class DateRange(val start: LocalDate, val end: LocalDate) {
    /** Number of days, counting both start and end. */
    val days: Int get() = ChronoUnit.DAYS.between(start, end).toInt() + 1

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end
}

/**
 * Splits the 7-day week that starts on [weekStart] at the month boundary (D17).
 * A normal week -> 1 segment. A week that crosses into a new month -> 2 segments.
 */
fun weekSegments(weekStart: LocalDate): List<DateRange> {
    val weekEnd = weekStart.plusDays(6)
    if (weekStart.month == weekEnd.month) {
        return listOf(DateRange(weekStart, weekEnd))
    }
    val lastDayOfMonth = weekStart.withDayOfMonth(weekStart.lengthOfMonth())
    return listOf(
        DateRange(weekStart, lastDayOfMonth),
        DateRange(lastDayOfMonth.plusDays(1), weekEnd),
    )
}
