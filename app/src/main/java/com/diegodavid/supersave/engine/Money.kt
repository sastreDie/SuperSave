package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.FixedPayment
import java.time.LocalDate

// freeMoney: written by Diego.
fun freeMoney(spendable: Long, upcomingPayments: List<Long>, savingsGoal: Long): Long {
    return spendable - upcomingPayments.sum() - savingsGoal
}

// ---- Everything below: written by Claude at Diego's request (formulas designed by Diego). ----
// Rounding rule (D26): divisions round DOWN (floorDiv), never up. A positive budget
// loses the leftover cents, which stay in free money; a negative one rounds further
// below zero. Either way the app never shows more money than there really is.

/**
 * The share of [freeMoney] that belongs to one segment of a week (D17).
 * freeMoney × segmentDays ÷ daysLeftInMonth. Multiply first: with whole numbers,
 * 7 / 30 would already be 0.
 */
fun segmentBudget(freeMoney: Long, segmentDays: Int, daysLeftInMonth: Int): Long {
    require(segmentDays in 1..daysLeftInMonth) { "segment must fit in the days left" }
    return Math.floorDiv(freeMoney * segmentDays, daysLeftInMonth.toLong())
}

/** The week's budget is the sum of its segments (one or two). */
fun weekBudget(segmentBudgets: List<Long>): Long = segmentBudgets.sum()

/** The main number on screen (D3). [spent] is a positive amount: ₱250 spent = 25_000. */
fun leftThisWeek(weekBudget: Long, spent: Long): Long = weekBudget - spent

/**
 * How much of a balance correction goes to the current segment, starting today (D18).
 * [difference] is real balance − tracked balance (negative if the app had too much).
 * [daysLeftInSegment] and [daysLeftInMonth] both count today.
 */
fun correctionForSegment(difference: Long, daysLeftInSegment: Int, daysLeftInMonth: Int): Long {
    require(daysLeftInSegment in 1..daysLeftInMonth) { "segment must fit in the days left" }
    return Math.floorDiv(difference * daysLeftInSegment, daysLeftInMonth.toLong())
}

/**
 * The amounts of fixed payments that fall inside [range], one entry per occurrence
 * (a quincena payment can appear twice). Cancelled payments stop counting from their
 * cancelledOn date. This is the `upcomingPayments` list freeMoney needs.
 */
fun paymentsDueIn(payments: List<FixedPayment>, range: DateRange): List<Long> {
    val amounts = mutableListOf<Long>()
    for (payment in payments) {
        for (date in occurrencesIn(payment.schedule, range)) {
            val cancelled = payment.cancelledOn
            if (cancelled == null || date < cancelled) amounts.add(payment.amount)
        }
    }
    return amounts
}

/** Which D6 message to show, comparing this week's budget with last week's. */
enum class BudgetTrend { UP, DOWN, SAME }

fun budgetTrend(thisWeek: Long, lastWeek: Long): BudgetTrend = when {
    thisWeek > lastWeek -> BudgetTrend.UP
    thisWeek < lastWeek -> BudgetTrend.DOWN
    else -> BudgetTrend.SAME
}

/** The rest of the month from [today], today included. Used for "still due this month". */
fun restOfMonth(today: LocalDate): DateRange =
    DateRange(today, today.withDayOfMonth(today.lengthOfMonth()))
