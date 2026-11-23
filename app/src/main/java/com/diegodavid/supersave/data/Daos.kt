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

    /**
     * The activity log on home: every row between two dates, newest first, except the
     * savings halves of extra money (they're shown as part of their income row).
     */
    @Query("SELECT * FROM ledger WHERE date BETWEEN :start AND :end AND linkedEntryId IS NULL ORDER BY date DESC, id DESC")
    suspend fun activityBetween(start: LocalDate, end: LocalDate): List<LedgerEntry>

    /** Ids of the rows that have a linked savings half (extra money that was saved). */
    @Query("SELECT linkedEntryId FROM ledger WHERE linkedEntryId IS NOT NULL AND date BETWEEN :start AND :end")
    suspend fun savedLinksBetween(start: LocalDate, end: LocalDate): List<Long>

    @Query("SELECT * FROM ledger WHERE id = :id")
    suspend fun get(id: Long): LedgerEntry?

    @Update
    suspend fun update(entry: LedgerEntry)

    /** Removes a row and its linked savings half, if it has one. */
    @Query("DELETE FROM ledger WHERE id = :id OR linkedEntryId = :id")
    suspend fun deleteWithLinked(id: Long)

    @Query("UPDATE ledger SET amount = :amount WHERE linkedEntryId = :id")
    suspend fun updateLinkedAmount(id: Long, amount: Long)

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

    /** The first week the app ever calculated: month close starts from its month. */
    @Query("SELECT MIN(start) FROM segments")
    suspend fun earliestStart(): LocalDate?

    /** D18: the one way a frozen budget changes, a balance correction. */
    @Query("UPDATE segments SET budget = budget + :delta WHERE start = :start")
    suspend fun addToBudget(start: LocalDate, delta: Long)
}

@Dao
interface MonthReviewDao {
    /** IGNORE: a month is closed only once. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(review: MonthReview)

    @Query("SELECT * FROM month_reviews WHERE month = :month")
    suspend fun get(month: java.time.YearMonth): MonthReview?

    /** The newest closed month the user hasn't reviewed yet (D9), or null. */
    @Query("SELECT * FROM month_reviews WHERE reviewed = 0 ORDER BY month DESC LIMIT 1")
    suspend fun latestUnreviewed(): MonthReview?

    @Update
    suspend fun update(review: MonthReview)

    /** After reviewing a month, older unreviewed ones keep their automatic choice. */
    @Query("UPDATE month_reviews SET reviewed = 1 WHERE month < :month")
    suspend fun markReviewedBefore(month: java.time.YearMonth)
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
