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
import com.diegodavid.supersave.AppClock
import com.diegodavid.supersave.engine.monthCloseTransfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.YearMonth
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

/** Everything the home screen and the summary widget show (D32). Amounts in cents. */
data class HomeData(
    val week: WeekSummary,
    /** This week's expenses, newest first. */
    val expenses: List<LedgerEntry>,
    /** What SuperSave thinks is in the account today (D14). */
    val balance: Long,
    /**
     * Free money for the rest of the month: spendable money minus payments still due
     * and the savings goal. It includes what's left this week. Can be negative (D25).
     */
    val leftThisMonth: Long,
    /** The savings bucket (D24): money set aside, inside the balance. */
    val saved: Long,
    /** This month's savings goal, already set aside from leftThisMonth. */
    val goal: Long,
)

/**
 * The one place the app and the widget ask "how is this week going?".
 * Loads data with the DAOs and hands it to the engine. (Plumbing by Claude, Nivel 2.)
 */
class BudgetRepository(private val db: SuperSaveDatabase) {

    // ---------- Reading ----------

    /**
     * Emits again every time a table that affects the numbers changes, or the date
     * changes (the debug "pretend today", AppClock).
     */
    private fun <T> whenDataChanges(load: suspend (today: LocalDate) -> T): Flow<T> =
        combine(
            db.invalidationTracker.createFlow("ledger", "settings", "fixed_payments", "recurring_incomes"),
            AppClock.pretend,
        ) { _, _ -> }.map { load(AppClock.today()) }

    fun observeWeek(): Flow<WeekSummary?> = whenDataChanges { currentWeek(it) }

    fun observeHome(): Flow<HomeData?> = whenDataChanges { home(it) }

    fun observeSettings(): Flow<Settings?> = db.settings().observe()
    fun observeIncomes(): Flow<List<RecurringIncome>> = db.recurringIncomes().observeActive()
    fun observePayments(): Flow<List<FixedPayment>> = db.fixedPayments().observeActive()

    suspend fun home(today: LocalDate): HomeData? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
        val week = currentWeek(today) ?: return@withTransaction null
        HomeData(
            week = week,
            expenses = db.ledger().expensesBetween(week.week.start, week.week.end),
            balance = balanceOn(today),
            leftThisMonth = freeMoneyNow(today, settings),
            saved = savedOn(today),
            goal = settings.monthlySavingsGoal,
        )
    }

    /** This week's numbers, or null if the app hasn't been set up yet. */
    suspend fun currentWeek(today: LocalDate): WeekSummary? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
        recordWhatCameDue(today)
        // Before freezing this week: a new month must first close the old one, so the
        // savings it moves aren't counted as this month's spending money (D24).
        closeFinishedMonths(today, settings)

        val weekStart = startOfWeek(today, settings.weekStartDayOfWeek)
        val week = DateRange(weekStart, weekStart.plusDays(6))
        val payments = db.fixedPayments().all()

        // Freeze every segment of this week that has started and isn't stored yet (D3, D20).
        // A segment that starts later in the week (the 1st) adds nothing until that day.
        val budgets = weekSegments(weekStart)
            .filter { it.start <= today }
            .map { segment ->
                val frozen = db.segments().get(segment.start)?.budget ?: freeze(segment, payments, settings)
                // A part of the week in a month that has already closed (Nov 30 seen on Dec 1):
                // its unspent budget went to savings at month close (D33), so it can't count
                // as "left this week" too. It only counts what was actually spent in it.
                if (YearMonth.from(segment.start) < YearMonth.from(today)) {
                    db.ledger().spentBetween(segment.start, segment.end)
                } else frozen
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

    /**
     * Month close (D9, D21, D24, D33). Every month before [today]'s month that has no
     * MonthReview yet is closed with the default choice from Settings, oldest first
     * (each close changes the money the next month starts with).
     */
    private suspend fun closeFinishedMonths(today: LocalDate, settings: Settings) {
        // Months start counting from the first week the app ever calculated.
        val first = db.segments().earliestStart() ?: return
        var month = YearMonth.from(first)
        val current = YearMonth.from(today)
        while (month < current) {
            if (db.monthReviews().get(month) == null) closeMonth(month, settings)
            month = month.plusMonths(1)
        }
    }

    /**
     * Moves the month's savings into the savings bucket with a SAVINGS_TRANSFER dated
     * its last day: the goal, plus the surplus when the choice is Keep it.
     * The surplus is what's left of the month's LAST segment budget. Money that arrived
     * during that last segment (e.g. salary on the 30th) was never part of this month's
     * budget, so it stays spendable for next month (D20).
     */
    private suspend fun closeMonth(month: YearMonth, settings: Settings) {
        val lastDay = month.atEndOfMonth()
        val last = segmentOf(lastDay, settings)
        // If the app wasn't opened during that segment, freeze it now, as it would have been.
        val budget = db.segments().get(last.start)?.budget ?: freeze(last, db.fixedPayments().all(), settings)
        val leftover = budget - db.ledger().spentBetween(last.start, last.end)
        val keep = settings.defaultReviewChoice == ReviewChoice.KEEP
        val toSavings = monthCloseTransfer(settings.monthlySavingsGoal, leftover, keep)

        db.monthReviews().insert(
            MonthReview(month, savingsGoal = settings.monthlySavingsGoal, surplus = leftover.coerceAtLeast(0))
        )
        if (toSavings > 0) {
            db.ledger().insert(LedgerEntry(date = lastDay, amount = toSavings, type = EntryType.SAVINGS_TRANSFER))
        }
    }

    /** Everything in the account at the end of [today] (savings included, D24). */
    private suspend fun balanceOn(today: LocalDate): Long = db.ledger().balanceBefore(today.plusDays(1))

    /** The savings bucket at the end of [today] (D24). */
    private suspend fun savedOn(today: LocalDate): Long = db.ledger().savedBefore(today.plusDays(1))

    /**
     * Free money right now, for the rest of the month (D25 formula, as of today).
     * Today's payments are already in the balance (recordWhatCameDue), so only later ones count.
     */
    private suspend fun freeMoneyNow(today: LocalDate, settings: Settings): Long {
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val stillDue = if (today < monthEnd) {
            paymentsDueIn(db.fixedPayments().all(), DateRange(today.plusDays(1), monthEnd))
        } else emptyList()
        return freeMoney(balanceOn(today) - savedOn(today), stillDue, settings.monthlySavingsGoal)
    }

    /** The part of the week that contains [today] (D17). */
    private fun segmentOf(today: LocalDate, settings: Settings): DateRange =
        weekSegments(startOfWeek(today, settings.weekStartDayOfWeek)).first { today in it }

    // ---------- Weigh the payment (D29) ----------

    /** "What if I pay [amount] today?" Nothing is saved. */
    suspend fun weigh(amount: Long, today: LocalDate): Weighing? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
        val week = currentWeek(today) ?: return@withTransaction null
        val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
        val daysAfter = ChronoUnit.DAYS.between(segmentOf(today, settings).end, monthEnd).toInt()
        weighPayment(amount, week.left, freeMoneyNow(today, settings), daysAfter)
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
     * Surprise money (D7, D31): a one-off INCOME row with no incomeId.
     * [save] = true: the same amount also goes into the savings bucket (D24), so it's
     * in the balance but not in spendable money, and the budget doesn't change.
     * [save] = false: it becomes spendable money. Like any income it counts from the
     * next segment (D4), spread over the rest of the month.
     */
    suspend fun addSurpriseMoney(amount: Long, note: String?, save: Boolean, today: LocalDate) = db.withTransaction {
        db.ledger().insert(LedgerEntry(date = today, amount = amount, type = EntryType.INCOME, note = note))
        if (save) {
            db.ledger().insert(LedgerEntry(date = today, amount = amount, type = EntryType.SAVINGS_TRANSFER, note = note))
        }
    }

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
