package com.diegodavid.supersave.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** A salary, beca, etc. that repeats on a schedule (D7). */
@Entity(tableName = "recurring_incomes")
data class RecurringIncome(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    @Embedded(prefix = "schedule_") val schedule: Schedule,
    // Optional, e.g. "Deposited to Nu".
    val note: String? = null,
    // null = still active.
    val endedOn: LocalDate? = null
)
