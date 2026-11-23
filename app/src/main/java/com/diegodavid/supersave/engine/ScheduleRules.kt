package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Calendar rules for the schedule presets (D8, D23). Written by Claude, Nivel 2.
// They only answer "does it happen on this date?". Whether an income has ended or
// a payment was cancelled (endedOn / cancelledOn) is checked by the caller.

/** True if [schedule] has an occurrence on [date]. Nothing happens before anchorDate. */
fun occursOn(schedule: Schedule, date: LocalDate): Boolean {
    if (date < schedule.anchorDate) return false
    val lastDay = date.lengthOfMonth()
    return when (schedule.type) {
        ScheduleType.END_OF_MONTH -> date.dayOfMonth == lastDay
        ScheduleType.START_OF_MONTH -> date.dayOfMonth == 1
        // Quincena: the 15th and the last day of the month.
        ScheduleType.BIWEEKLY -> date.dayOfMonth == 15 || date.dayOfMonth == lastDay
        ScheduleType.EVERY_N_DAYS -> {
            val n = schedule.everyNDays ?: return false
            n > 0 && ChronoUnit.DAYS.between(schedule.anchorDate, date) % n == 0L
        }
        ScheduleType.DAY_OF_MONTH -> {
            val day = schedule.dayOfMonth ?: return false
            // "The 31st" in a 30-day month means the last day (DATA_MODEL.md).
            date.dayOfMonth == minOf(day, lastDay)
        }
    }
}

/** Every date in [range] on which [schedule] occurs, in order. */
fun occurrencesIn(schedule: Schedule, range: DateRange): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = range.start
    while (current <= range.end) {
        if (occursOn(schedule, current)) dates.add(current)
        current = current.plusDays(1)
    }
    return dates
}
