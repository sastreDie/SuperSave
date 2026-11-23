package com.diegodavid.supersave.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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

    /** The expense rows themselves, newest first (the list on the home screen). */
    @Query("SELECT * FROM ledger WHERE type = 'EXPENSE' AND date BETWEEN :start AND :end ORDER BY date DESC, id DESC")
    suspend fun expensesBetween(start: LocalDate, end: LocalDate): List<LedgerEntry>

    /** Only expenses can be deleted by hand; the other rows are history. */
    @Query("DELETE FROM ledger WHERE id = :id AND type = 'EXPENSE'")
    suspend fun deleteExpense(id: Long)

    /** Dates a fixed payment was already deducted, so it's never deducted twice. */
    @Query("SELECT date FROM ledger WHERE paymentId = :paymentId")
    suspend fun datesForPayment(paymentId: Long): List<LocalDate>

    /** Dates a recurring income was already added. */
    @Query("SELECT date FROM ledger WHERE incomeId = :incomeId")
    suspend fun datesForIncome(incomeId: Long): List<LocalDate>
}

@Dao
interface FixedPaymentDao {
    @Insert
    suspend fun insert(payment: FixedPayment): Long

    @Update
    suspend fun update(payment: FixedPayment)

    /** All of them, cancelled ones too: the engine needs them for past dates. */
    @Query("SELECT * FROM fixed_payments")
    suspend fun all(): List<FixedPayment>

    /** The ones still expected, for the settings screen. */
    @Query("SELECT * FROM fixed_payments WHERE cancelledOn IS NULL ORDER BY name")
    fun observeActive(): Flow<List<FixedPayment>>

    /** D15: stop expecting it from [date] on. The row stays so past deductions keep their link. */
    @Query("UPDATE fixed_payments SET cancelledOn = :date WHERE id = :id")
    suspend fun cancel(id: Long, date: LocalDate)
}

@Dao
interface RecurringIncomeDao {
    @Insert
    suspend fun insert(income: RecurringIncome): Long

    @Update
    suspend fun update(income: RecurringIncome)

    @Query("SELECT * FROM recurring_incomes")
    suspend fun all(): List<RecurringIncome>

    @Query("SELECT * FROM recurring_incomes WHERE endedOn IS NULL ORDER BY name")
    fun observeActive(): Flow<List<RecurringIncome>>

    @Query("UPDATE recurring_incomes SET endedOn = :date WHERE id = :id")
    suspend fun end(id: Long, date: LocalDate)
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

    /** D18: the one way a frozen budget changes, a balance correction. */
    @Query("UPDATE segments SET budget = budget + :delta WHERE start = :start")
    suspend fun addToBudget(start: LocalDate, delta: Long)
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
