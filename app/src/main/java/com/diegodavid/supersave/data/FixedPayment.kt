package com.diegodavid.supersave.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Rent, subscriptions, etc. Auto-deducted on its date (D15). */
@Entity(tableName = "fixed_payments")
data class FixedPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Long,
    @Embedded(prefix = "schedule_") val schedule: Schedule,
    // Optional, e.g. "Paid with the BBVA card".
    val note: String? = null,
    // null = active. A date instead of deleting, so past deductions keep their link.
    val cancelledOn: LocalDate? = null
)
