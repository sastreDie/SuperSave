package com.diegodavid.supersave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// DAOs ("Data Access Objects"): the SQL queries, written by Claude (Nivel 2 plumbing).
// You write an interface with the SQL in @Query; Room generates the code that runs it.
// `suspend` = Room runs the query on a background thread, so the screen never freezes.
// :name in the SQL = the function parameter with that name.
// Dates are stored as ISO text ("2026-11-22"), so < and BETWEEN compare them correctly.

@Dao
interface LedgerDao {
    @Insert
    suspend fun insert(entry: LedgerEntry): Long

    @Insert
    suspend fun insertAll(entries: List<LedgerEntry>)

    /** Account balance from everything dated before [date] (D4: today's money counts next week). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE type != 'SAVINGS_TRANSFER' AND date < :date")
    suspend fun balanceBefore(date: LocalDate): Long

    /** Savings bucket before [date] (D24). */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE type = 'SAVINGS_TRANSFER' AND date < :date")
    suspend fun savedBefore(date: LocalDate): Long

    /** Expenses between two dates (both included), as a positive number. */
    @Query("SELECT COALESCE(-SUM(amount), 0) FROM ledger WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end")
    suspend fun spentBetween(start: LocalDate, end: LocalDate): Long
}

@Dao
interface FixedPaymentDao {
    @Insert
    suspend fun insert(payment: FixedPayment): Long

    @Query("SELECT * FROM fixed_payments")
    suspend fun all(): List<FixedPayment>
}

@Dao
interface RecurringIncomeDao {
    @Insert
    suspend fun insert(income: RecurringIncome): Long

    @Query("SELECT * FROM recurring_incomes")
    suspend fun all(): List<RecurringIncome>
}

@Dao
interface SegmentDao {
    /** IGNORE: a frozen segment is never overwritten by accident (D3). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(segment: Segment)

    @Query("SELECT * FROM segments WHERE start = :start")
    suspend fun get(start: LocalDate): Segment?

    /** Segments that start inside a date range, e.g. last week's. */
    @Query("SELECT * FROM segments WHERE start BETWEEN :from AND :to ORDER BY start")
    suspend fun startingBetween(from: LocalDate, to: LocalDate): List<Segment>
}

@Dao
interface SettingsDao {
    /** null = the app hasn't been set up yet. */
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): Settings?

    /** Same as get(), but emits again whenever the row changes (setup done, data cleared). */
    @Query("SELECT * FROM settings WHERE id = 1")
    fun observe(): Flow<Settings?>

    /** Upsert = INSERT, or UPDATE if the row already exists. */
    @Upsert
    suspend fun save(settings: Settings)
}
