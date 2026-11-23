package com.diegodavid.supersave.data

import androidx.room.withTransaction
import com.diegodavid.supersave.engine.DateRange
import com.diegodavid.supersave.engine.WeekSummary
import com.diegodavid.supersave.engine.Weighing
import com.diegodavid.supersave.engine.budgetTrend
import com.diegodavid.supersave.engine.correctionForSegment
import com.diegodavid.supersave.engine.daysLeftInMonth
import com.diegodavid.supersave.engine.dueDates
import com.diegodavid.supersave.engine.freeMoney
import com.diegodavid.supersave.engine.freezeSegmentBudget
import com.diegodavid.supersave.engine.leftThisWeek
import com.diegodavid.supersave.engine.paydayIn
import com.diegodavid.supersave.engine.paymentNamesDueIn
import com.diegodavid.supersave.engine.paymentsDueIn
import com.diegodavid.supersave.engine.startOfWeek
import com.diegodavid.supersave.engine.weekBudget
import com.diegodavid.supersave.engine.weekSegments
import com.diegodavid.supersave.engine.weighPayment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What the setup screens collect (D2, D7, D8, D14, D21). Amounts in cents. */
data class SetupData(
    val weekStart: DayOfWeek,
    val startingBalance: Long,
    val incomes: List<RecurringIncome>,
    val payments: List<FixedPayment>,
    val monthlySavingsGoal: Long,
    val defaultReviewChoice: ReviewChoice,
)

/** Everything the home screen shows. */
data class HomeData(
    val week: WeekSummary,
    /** This week's expenses, newest first. */
    val expenses: List<LedgerEntry>,
    /** What SuperSave thinks is in the account today (D14). */
    val balance: Long,
)

/**
 * The one place the app and the widget ask "how is this week going?".
 * Loads data with the DAOs and hands it to the engine. (Plumbing by Claude, Nivel 2.)
 */
class BudgetRepository(private val db: SuperSaveDatabase) {

    // ---------- Reading ----------

    /** Emits again every time a table that affects the numbers changes. */
    private fun <T> whenDataChanges(load: suspend () -> T): Flow<T> =
        db.invalidationTracker
            .createFlow("ledger", "settings", "fixed_payments", "recurring_incomes")
            .map { load() }

    fun observeWeek(): Flow<WeekSummary?> = whenDataChanges { currentWeek(LocalDate.now()) }

    fun observeHome(): Flow<HomeData?> = whenDataChanges { home(LocalDate.now()) }

    fun observeSettings(): Flow<Settings?> = db.settings().observe()
    fun observeIncomes(): Flow<List<RecurringIncome>> = db.recurringIncomes().observeActive()
    fun observePayments(): Flow<List<FixedPayment>> = db.fixedPayments().observeActive()

    suspend fun home(today: LocalDate): HomeData? = db.withTransaction {
        val week = currentWeek(today) ?: return@withTransaction null
        HomeData(
            week = week,
            expenses = db.ledger().expensesBetween(week.week.start, week.week.end),
            balance = balanceOn(today),
        )
    }

    /** This week's numbers, or null if the app hasn't been set up yet. */
    suspend fun currentWeek(today: LocalDate): WeekSummary? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
        recordWhatCameDue(today)

        val weekStart = startOfWeek(today, settings.weekStartDayOfWeek)
        val week = DateRange(weekStart, weekStart.plusDays(6))
        val payments = db.fixedPayments().all()

        // Freeze every segment of this week that has started and isn't stored yet (D3, D20).
        // A segment that starts later in the week (the 1st) adds nothing until that day.
        val budgets = weekSegments(weekStart)
            .filter { it.start <= today }
            .map { segment ->
                db.segments().get(segment.start)?.budget ?: freeze(segment, payments, settings)
            }

        val budget = weekBudget(budgets)
        val spent = db.ledger().spentBetween(week.start, week.end)
        val lastWeek = db.segments()
            .startingBetween(weekStart.minusDays(7), weekStart.minusDays(1))
            .map { it.budget }
            .takeIf { it.isNotEmpty() }
            ?.sum()

        WeekSummary(
            week = week,
            budget = budget,
            spent = spent,
            left = leftThisWeek(budget, spent),
            lastWeekBudget = lastWeek,
            trend = lastWeek?.let { budgetTrend(budget, it) },
            payday = paydayIn(db.recurringIncomes().all(), week),
            paymentsDue = paymentNamesDueIn(payments, week),
        )
    }

    private suspend fun freeze(segment: DateRange, payments: List<FixedPayment>, settings: Settings): Long {
        val budget = freezeSegmentBudget(
            segment = segment,
            balanceBefore = db.ledger().balanceBefore(segment.start),
            savedBefore = db.ledger().savedBefore(segment.start),
            payments = payments,
            monthlySavingsGoal = settings.monthlySavingsGoal,
        )
        db.segments().insert(Segment(segment.start, segment.end, budget))
        return budget
    }

    /**
     * D15 without a background job: every time the app or the widget calculates, any
     * fixed payment or recurring income that came due up to today and isn't in the
     * ledger yet gets its row, dated the day it was due. So nothing is missed even
     * if the phone was off. See D30.
     */
    private suspend fun recordWhatCameDue(today: LocalDate) {
        val rows = mutableListOf<LedgerEntry>()
        for (p in db.fixedPayments().all()) {
            val done = db.ledger().datesForPayment(p.id).toSet()
            dueDates(p.schedule, p.cancelledOn, today).filter { it !in done }.forEach {
                rows += LedgerEntry(date = it, amount = -p.amount, type = EntryType.FIXED_PAYMENT, paymentId = p.id)
            }
        }
        for (i in db.recurringIncomes().all()) {
            val done = db.ledger().datesForIncome(i.id).toSet()
            dueDates(i.schedule, i.endedOn, today).filter { it !in done }.forEach {
                rows += LedgerEntry(date = it, amount = i.amount, type = EntryType.INCOME, incomeId = i.id)
            }
        }
        if (rows.isNotEmpty()) db.ledger().insertAll(rows)
    }

    /** Everything in the account at the end of [today] (savings included, D24). */
    private suspend fun balanceOn(today: LocalDate): Long = db.ledger().balanceBefore(today.plusDays(1))

    /** The part of the week that contains [today] (D17). */
    private fun segmentOf(today: LocalDate, settings: Settings): DateRange =
        weekSegments(startOfWeek(today, settings.weekStartDayOfWeek)).first { today in it }

    // ---------- Weigh the payment (D29) ----------

    /** "What if I pay [amount] today?" Nothing is saved. */
    suspend fun weigh(amount: Long, today: LocalDate): Weighing? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
        val week = currentWeek(today) ?: return@withTransaction null
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val spendable = balanceOn(today) - db.ledger().savedBefore(today.plusDays(1))
        // Today's payments are already in the balance (recordWhatCameDue), so only later ones.
        val stillDue = if (today < monthEnd) {
            paymentsDueIn(db.fixedPayments().all(), DateRange(today.plusDays(1), monthEnd))
        } else emptyList()
        val free = freeMoney(spendable, stillDue, settings.monthlySavingsGoal)
        val daysAfter = ChronoUnit.DAYS.between(segmentOf(today, settings).end, monthEnd).toInt()
        weighPayment(amount, week.left, free, daysAfter)
    }

    // ---------- Writing ----------

    /**
     * Saves everything from the setup screens in one go. Settings are written last,
     * and the app switches to the home screen as soon as the settings row exists.
     *
     * The starting balance is dated the day BEFORE the segment that contains [today],
     * so this week's budget can freeze with it (D27 only counts money dated before a
     * segment's start). Without that, setting up on a Wednesday would give a ₱0 week.
     * See D28.
     */
    suspend fun completeSetup(setup: SetupData, today: LocalDate) = db.withTransaction {
        val weekStart = startOfWeek(today, setup.weekStart)
        val currentSegment = weekSegments(weekStart).first { today in it }
        // The first BALANCE_CORRECTION is the starting balance (DATA_MODEL.md).
        db.ledger().insert(
            LedgerEntry(
                date = currentSegment.start.minusDays(1),
                amount = setup.startingBalance,
                type = EntryType.BALANCE_CORRECTION,
            )
        )
        setup.incomes.forEach { db.recurringIncomes().insert(it) }
        setup.payments.forEach { db.fixedPayments().insert(it) }
        db.settings().save(
            Settings(
                weekStartDayOfWeek = setup.weekStart,
                monthlySavingsGoal = setup.monthlySavingsGoal,
                defaultReviewChoice = setup.defaultReviewChoice,
            )
        )
    }

    suspend fun addExpense(date: LocalDate, amount: Long, note: String? = null) {
        // Stored negative: money out.
        db.ledger().insert(LedgerEntry(date = date, amount = -amount, type = EntryType.EXPENSE, note = note))
    }

    suspend fun deleteExpense(id: Long) = db.ledger().deleteExpense(id)

    /**
     * "Correct my balance" (D14, D18). Writes the difference as a BALANCE_CORRECTION
     * dated today and gives the current segment its share right away. Later segments
     * pick up the rest when they freeze, because it's in the balance by then.
     */
    suspend fun correctBalance(realBalance: Long, today: LocalDate) = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction
        // Freeze today's segment first, so the correction is added to it, not baked in twice.
        currentWeek(today)
        val difference = realBalance - balanceOn(today)
        if (difference == 0L) return@withTransaction
        db.ledger().insert(LedgerEntry(date = today, amount = difference, type = EntryType.BALANCE_CORRECTION))
        val segment = segmentOf(today, settings)
        val daysLeftInSegment = ChronoUnit.DAYS.between(today, segment.end).toInt() + 1
        db.segments().addToBudget(
            segment.start,
            correctionForSegment(difference, daysLeftInSegment, daysLeftInMonth(today)),
        )
    }

    // Settings. Frozen segments don't change (D3): edits count from the next segment.

    suspend fun saveSettings(settings: Settings) = db.settings().save(settings)

    suspend fun addIncome(income: RecurringIncome) = db.recurringIncomes().insert(income)
    suspend fun updateIncome(income: RecurringIncome) = db.recurringIncomes().update(income)
    suspend fun endIncome(id: Long, today: LocalDate) = db.recurringIncomes().end(id, today)

    suspend fun addPayment(payment: FixedPayment) = db.fixedPayments().insert(payment)
    suspend fun updatePayment(payment: FixedPayment) = db.fixedPayments().update(payment)
    suspend fun cancelPayment(id: Long, today: LocalDate) = db.fixedPayments().cancel(id, today)
}
