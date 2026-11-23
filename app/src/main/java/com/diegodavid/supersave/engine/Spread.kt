package com.diegodavid.supersave.engine

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Money coming in is spread over the next 30 days (D36, Diego). Written by Claude.
//
// A payday of ₱10,000 on a Sunday gives ₱333.33 to that Sunday, ₱2,333.33 to each of
// the next four weeks and the rest to the last two days: the same amount every day for
// 30 days, starting the day it arrives, whatever month those days are in.

const val SPREAD_DAYS = 30

/** Money that arrived on [date] and is spread over the 30 days starting that day. Cents. */
data class MoneyIn(val date: LocalDate, val amount: Long)

/**
 * How much of [money] belongs to the days up to and including [day].
 * Rounded down (D26) on the running total, so the 30 days always add up to the
 * exact amount: nothing is lost to rounding, the last day just gets the cents.
 */
fun releasedBy(money: MoneyIn, day: LocalDate): Long {
    val days = (ChronoUnit.DAYS.between(money.date, day) + 1).coerceIn(0, SPREAD_DAYS.toLong())
    return Math.floorDiv(money.amount * days, SPREAD_DAYS.toLong())
}

/** The part of [money] that belongs to the days in [range]. */
fun releasedIn(money: MoneyIn, range: DateRange): Long =
    releasedBy(money, range.end) - releasedBy(money, range.start.minusDays(1))

/** The part of [money] that belongs to days after [day]: not spendable yet. */
fun pendingAfter(money: MoneyIn, day: LocalDate): Long = money.amount - releasedBy(money, day)

fun releasedIn(money: List<MoneyIn>, range: DateRange): Long = money.sumOf { releasedIn(it, range) }
fun pendingAfter(money: List<MoneyIn>, day: LocalDate): Long = money.sumOf { pendingAfter(it, day) }
