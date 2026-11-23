package com.diegodavid.supersave.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Money coming in, spread over 30 days (D36). Tests by Claude. Amounts in cents. */
class SpreadTest {
    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)
    private fun week(start: LocalDate) = DateRange(start, start.plusDays(6))

    // Diego's example: ₱10,000 on the last day of a week (Sunday Nov 15, 2026).
    private val payday = MoneyIn(d(11, 15), 1_000_000L)

    @Test
    fun paydayOnSunday_givesOneDayToThatWeek() {
        assertEquals(33_333L, releasedIn(payday, DateRange(d(11, 9), d(11, 15))))
    }

    @Test
    fun nextFourWeeks_getSevenDaysEach() {
        // 10,000 × 7 ÷ 30 = 2,333.33; the running total keeps every week within a cent.
        assertEquals(233_333L, releasedIn(payday, week(d(11, 16))))
        assertEquals(233_334L, releasedIn(payday, week(d(11, 23))))
        assertEquals(233_333L, releasedIn(payday, week(d(11, 30))))
        assertEquals(233_333L, releasedIn(payday, week(d(12, 7))))
    }

    @Test
    fun lastDay_getsTheRest() {
        // Sunday + 4 full weeks = 29 days, so the week of Dec 14 gets only Dec 14,
        // with the leftover cent: 10,000 − 9,666.66 = 333.34.
        assertEquals(33_334L, releasedIn(payday, week(d(12, 14))))
    }

    @Test
    fun thirtyDaysAddUpExactly_crossingTheMonth() {
        val all = DateRange(d(11, 1), d(12, 31))
        assertEquals(1_000_000L, releasedIn(payday, all))
        // Nothing left after the 30th day (Dec 14).
        assertEquals(0L, pendingAfter(payday, d(12, 14)))
    }

    @Test
    fun nothingBeforeItArrives() {
        assertEquals(0L, releasedIn(payday, DateRange(d(11, 1), d(11, 14))))
        assertEquals(1_000_000L, pendingAfter(payday, d(11, 14)))
    }

    @Test
    fun pending_isWhatsLeftForLaterDays() {
        // After Nov 30 (16 days in): 10,000 − 10,000 × 16 ÷ 30 = 4,666.67 still to come.
        assertEquals(466_667L, pendingAfter(payday, d(11, 30)))
    }

    @Test
    fun freeze_leavesPendingMoneyOutOfTheBase() {
        // ₱5,000 in the account, ₱3,000 of it still pending: the base only sees ₱2,000.
        val segment = DateRange(d(11, 16), d(11, 22))
        val withPending = freezeSegmentBudget(segment, 500_000L, 0L, emptyList(), 0L, pendingBefore = 300_000L)
        val without = freezeSegmentBudget(segment, 200_000L, 0L, emptyList(), 0L)
        assertEquals(without, withPending)
    }
}
