package com.diegodavid.supersave.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Every movement of money (the "Transaction" table in DATA_MODEL.md, renamed:
 * Room already has an @Transaction annotation and TRANSACTION is an SQL keyword).
 * Balance = sum of all amounts except SAVINGS_TRANSFER (D24).
 */
@Entity(
    tableName = "ledger",
    // Most queries ask "entries between these dates", so the date column is indexed.
    indices = [Index("date")]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    // Cents. Positive = money in, negative = money out.
    val amount: Long,
    val type: EntryType,
    val note: String? = null,
    // Set when a RecurringIncome / FixedPayment created this entry automatically.
    val incomeId: Long? = null,
    val paymentId: Long? = null,
    // Database v2: a SAVINGS_TRANSFER that belongs to another row, e.g. the "save it"
    // half of extra money (D31). Editing or removing that row changes this one too.
    val linkedEntryId: Long? = null,
)
