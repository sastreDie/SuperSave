package com.diegodavid.supersave.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * TEMPORARY: fake November 2026 data so the debug screen and the widget have real
 * numbers before the setup screens exist. Delete once setup works. (Claude, Nivel 2.)
 */
object TestData {
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)
    private val longAgo = d(1, 1)

    /** Wipes every table. Blocking call, so it runs on a background (IO) thread. */
    suspend fun clear(db: SuperSaveDatabase) = withContext(Dispatchers.IO) { db.clearAllTables() }

    suspend fun load(db: SuperSaveDatabase) {
        clear(db)
        db.withTransaction { insertNovember(db) }
    }

    private suspend fun insertNovember(db: SuperSaveDatabase) {
        db.settings().save(Settings(monthlySavingsGoal = 200_000L))   // $2,000 goal, Monday weeks

        val rent = db.fixedPayments().insert(FixedPayment(name = "Rent", amount = 300_000L,
            schedule = Schedule(ScheduleType.START_OF_MONTH, longAgo)))
        val claude = db.fixedPayments().insert(FixedPayment(name = "Claude", amount = 39_900L,
            schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 10)))
        db.fixedPayments().insert(FixedPayment(name = "Internet", amount = 50_000L,
            schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 28)))
        val salary = db.recurringIncomes().insert(RecurringIncome(name = "Salary", amount = 600_000L,
            schedule = Schedule(ScheduleType.BIWEEKLY, longAgo)))

        db.ledger().insertAll(listOf(
            LedgerEntry(date = d(10, 31), amount = 500_000L, type = EntryType.BALANCE_CORRECTION, note = "Starting balance"),
            // The auto-deductions WorkManager will make later (D15), written by hand for now.
            LedgerEntry(date = d(11, 1), amount = -300_000L, type = EntryType.FIXED_PAYMENT, paymentId = rent),
            LedgerEntry(date = d(11, 10), amount = -39_900L, type = EntryType.FIXED_PAYMENT, paymentId = claude),
            LedgerEntry(date = d(11, 15), amount = 600_000L, type = EntryType.INCOME, incomeId = salary),
            LedgerEntry(date = d(11, 3), amount = -45_000L, type = EntryType.EXPENSE, note = "Tacos"),
            LedgerEntry(date = d(11, 8), amount = -120_000L, type = EntryType.EXPENSE, note = "Groceries"),
            LedgerEntry(date = d(11, 12), amount = -30_000L, type = EntryType.EXPENSE, note = "Uber"),
            LedgerEntry(date = d(11, 17), amount = -25_000L, type = EntryType.EXPENSE, note = "Coffee"),
            LedgerEntry(date = d(11, 19), amount = -60_000L, type = EntryType.EXPENSE, note = "Groceries"),
            LedgerEntry(date = d(11, 21), amount = -18_000L, type = EntryType.EXPENSE, note = "Snacks"),
        ))
        // Last week's frozen budget, so the D6 message has something to compare with.
        db.segments().insert(Segment(d(11, 9), d(11, 15), 130_000L))
    }
}
