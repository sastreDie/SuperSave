package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.FixedPayment
import com.diegodavid.supersave.data.RecurringIncome
import java.time.LocalDate

// Glue between the money formulas and the data. Written by Claude at Diego's request.
// Pure functions (no database), so they're tested in WeekPlannerTest.

/**
 * The budget a segment gets when it freezes at its start (D3, D20).
 * [balanceBefore] and [savedBefore] only include ledger entries dated before
 * segment.start, so money received on or after that day waits for the next segment (D4).
 */
fun freezeSegmentBudget(
    segment: DateRange,
    balanceBefore: Long,
    savedBefore: Long,
    payments: List<FixedPayment>,
    monthlySavingsGoal: Long,
): Long {
    val free = freeMoney(
        spendable = balanceBefore - savedBefore,
        upcomingPayments = paymentsDueIn(payments, restOfMonth(segment.start)),
        savingsGoal = monthlySavingsGoal,
    )
    return segmentBudget(free, segment.days, daysLeftInMonth(segment.start))
}

/** "Pay day this week!" (D13): an active recurring income falls inside [week]. */
fun paydayIn(incomes: List<RecurringIncome>, week: DateRange): Boolean =
    incomes.any { income ->
        occurrencesIn(income.schedule, week).any { date ->
            val ended = income.endedOn
            ended == null || date < ended
        }
    }

/** Names of the fixed payments due inside [week] (D13), e.g. ["Claude"]. */
fun paymentNamesDueIn(payments: List<FixedPayment>, week: DateRange): List<String> =
    payments.filter { paymentsDueIn(listOf(it), week).isNotEmpty() }.map { it.name }

/** Everything a screen or the widget needs to show this week. */
data class WeekSummary(
    val week: DateRange,
    /** Sum of the segments that have started. A split week grows on the 1st (D20). */
    val budget: Long,
    val spent: Long,
    val left: Long,
    /** null if last week was never calculated (e.g. the app wasn't used). */
    val lastWeekBudget: Long?,
    val trend: BudgetTrend?,
    val payday: Boolean,
    val paymentsDue: List<String>,
)
