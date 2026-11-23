package com.diegodavid.supersave.data

import java.time.LocalDate

/**
 * Not a table: it gets @Embedded inside RecurringIncome and FixedPayment,
 * so both tables share the same schedule columns (DATA_MODEL.md).
 */
data class Schedule(
    val type: ScheduleType,
    // First occurrence; for EVERY_N_DAYS, the date the count starts from.
    val anchorDate: LocalDate,
    // Only for EVERY_N_DAYS.
    val everyNDays: Int? = null,
    // Only for DAY_OF_MONTH, 1..31 (31 in a 30-day month = last day).
    val dayOfMonth: Int? = null
)
