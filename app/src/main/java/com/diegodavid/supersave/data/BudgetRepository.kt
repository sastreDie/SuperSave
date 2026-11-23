package com.diegodavid.supersave.data

import androidx.room.withTransaction
import com.diegodavid.supersave.engine.DateRange
import com.diegodavid.supersave.engine.WeekSummary
import com.diegodavid.supersave.engine.budgetTrend
import com.diegodavid.supersave.engine.freezeSegmentBudget
import com.diegodavid.supersave.engine.leftThisWeek
import com.diegodavid.supersave.engine.paydayIn
import com.diegodavid.supersave.engine.paymentNamesDueIn
import com.diegodavid.supersave.engine.startOfWeek
import com.diegodavid.supersave.engine.weekBudget
import com.diegodavid.supersave.engine.weekSegments
import java.time.DayOfWeek
import java.time.LocalDate

/** What the setup screens collect (D2, D7, D8, D14, D21). Amounts in cents. */
data class SetupData(
    val weekStart: DayOfWeek,
    val startingBalance: Long,
    val incomes: List<RecurringIncome>,
    val payments: List<FixedPayment>,
    val monthlySavingsGoal: Long,
    val defaultReviewChoice: ReviewChoice,
)

/**
 * The one place the app and the widget ask "how is this week going?".
 * Loads data with the DAOs and hands it to the engine. (Plumbing by Claude, Nivel 2.)
 */
class BudgetRepository(private val db: SuperSaveDatabase) {

    /** This week's numbers, or null if the app hasn't been set up yet. */
    suspend fun currentWeek(today: LocalDate): WeekSummary? = db.withTransaction {
        val settings = db.settings().get() ?: return@withTransaction null
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
}
