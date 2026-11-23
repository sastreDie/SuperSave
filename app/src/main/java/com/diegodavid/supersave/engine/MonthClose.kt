package com.diegodavid.supersave.engine

import java.time.LocalDate
import java.time.YearMonth

// Month close (D9, D21, D24, D33). Written by Claude after Diego found that savings
// were never moved; the rule follows D9/D21 and is for Diego to confirm.

/**
 * The savings goal for [month] (D37). In the month the app was set up ([startedOn]),
 * only the days from setup on count: setting up on Nov 22 with a ₱1,000 goal asks for
 * ₱1,000 × 9/30 = ₱300 in November, then the full ₱1,000 from December. Rounded down (D26).
 */
fun goalForMonth(goal: Long, month: YearMonth, startedOn: LocalDate?): Long {
    if (startedOn == null || YearMonth.from(startedOn) != month) return goal
    val days = month.lengthOfMonth() - startedOn.dayOfMonth + 1
    return Math.floorDiv(goal * days, month.lengthOfMonth().toLong())
}

/**
 * How much goes into the savings bucket when a month closes. Amounts in cents.
 *
 * [goal]: the monthly savings goal, set aside all month.
 * [leftover]: what's left of the month's last segment budget (budget − spent).
 *   Negative when that last week was overspent.
 * [keep]: the choice for the surplus: true = Keep it (save it), false = Spend it.
 *
 * - The goal is always saved, minus any overspend of the last week, which comes
 *   out of savings because there are no weeks left to spread it over (D29).
 * - A positive leftover is the surplus: saved with Keep it; with Spend it, it stays
 *   spendable and grows next month's budget.
 * Never negative: closing a month never takes money out of savings.
 */
fun monthCloseTransfer(goal: Long, leftover: Long, keep: Boolean): Long {
    val goalPart = (goal + minOf(0L, leftover)).coerceAtLeast(0L)
    val surplusPart = if (keep) leftover.coerceAtLeast(0L) else 0L
    return goalPart + surplusPart
}
