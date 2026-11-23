package com.diegodavid.supersave.engine

// Weigh the payment (D29). Written by Claude at Diego's request; to be reviewed by Diego (D11).
// "What happens if I pay this?", computed without saving anything.

/** The answer to "what if I pay [amount] now?". All amounts in cents. */
data class Weighing(
    val amount: Long,
    /** Left this week after paying. Negative if it doesn't fit. */
    val leftAfter: Long,
    /** The part that doesn't fit in this week's budget (0 if it fits). */
    val overspend: Long,
    /**
     * How much lower each later week of the month gets, because the overspend is
     * spread over them (D5). null when there are no days left in the month after this week.
     */
    val perWeekLater: Long?,
    /** How much of this month's savings goal it would use (0 if none). D25. */
    val savingsDip: Long,
) {
    val fitsThisWeek: Boolean get() = overspend == 0L
}

/**
 * [leftThisWeek]: the number on the home screen right now.
 * [freeMoneyNow]: spendable money minus payments still due this month (after today)
 *   minus the savings goal. It already contains what's left this week.
 * [daysAfterThisWeek]: days of this month after the current part of the week (D17).
 *
 * The payment itself is never rounded: it's taken to the exact cent. The only
 * division is the per-week estimate, and it rounds DOWN like every other division
 * in the app (D26).
 */
fun weighPayment(amount: Long, leftThisWeek: Long, freeMoneyNow: Long, daysAfterThisWeek: Int): Weighing {
    require(amount > 0) { "a payment must cost something" }
    require(daysAfterThisWeek >= 0)
    val overspend = (amount - leftThisWeek.coerceAtLeast(0)).coerceAtLeast(0)
    val perWeekLater = if (overspend > 0 && daysAfterThisWeek > 0) {
        // Spread over the days left; a "week" is 7 days, or fewer if the month ends sooner.
        Math.floorDiv(overspend * minOf(7, daysAfterThisWeek), daysAfterThisWeek.toLong())
    } else null
    // Free money is what's left once bills and the goal are set aside. Paying more than
    // that has to come out of the goal. If free money is already negative, all of it does.
    val savingsDip = (amount - freeMoneyNow.coerceAtLeast(0)).coerceAtLeast(0)
    return Weighing(amount, leftThisWeek - amount, overspend, perWeekLater, savingsDip)
}
