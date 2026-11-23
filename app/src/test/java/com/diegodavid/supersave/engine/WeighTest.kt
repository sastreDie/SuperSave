package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Weigh the payment (D29) and recording what came due (D15). Tests by Claude. Amounts in cents. */
class WeighTest {

    @Test
    fun fitsThisWeek() {
        // $700 left, a $250 dinner. Plenty of free money for the month.
        val w = weighPayment(25_000L, leftThisWeek = 70_000L, freeMoneyNow = 300_000L, daysAfterThisWeek = 15)
        assertTrue(w.fitsThisWeek)
        assertEquals(45_000L, w.leftAfter)
        assertNull(w.perWeekLater)
        assertEquals(0L, w.savingsDip)
    }

    @Test
    fun overspend_isSpreadOverTheRestOfTheMonth() {
        // $700 left, a $1,000 jacket: $300 over. 15 days after this week:
        // 300 × 7 ÷ 15 = 140 per week.
        val w = weighPayment(100_000L, 70_000L, 300_000L, 15)
        assertFalse(w.fitsThisWeek)
        assertEquals(-30_000L, w.leftAfter)
        assertEquals(30_000L, w.overspend)
        assertEquals(14_000L, w.perWeekLater)
        assertEquals(0L, w.savingsDip)
    }

    @Test
    fun perWeekEstimateRoundsDown() {
        // $100 over, 30 days after: 100 × 7 ÷ 30 = 23.333... -> $23.33 (D26).
        assertEquals(2_333L, weighPayment(80_000L, 70_000L, 300_000L, 30).perWeekLater)
    }

    @Test
    fun paymentIsNeverRounded() {
        // An odd amount is taken to the exact cent.
        val w = weighPayment(12_345L, leftThisWeek = 70_000L, freeMoneyNow = 300_000L, daysAfterThisWeek = 15)
        assertEquals(70_000L - 12_345L, w.leftAfter)
    }

    @Test
    fun shortEndOfMonth_isNotMoreThanTheOverspend() {
        // Only 3 days left after this week: the "week" is those 3 days, so it gets all $300.
        assertEquals(30_000L, weighPayment(100_000L, 70_000L, 300_000L, 3).perWeekLater)
    }

    @Test
    fun lastWeekOfMonth_hasNoLaterWeeks() {
        assertNull(weighPayment(100_000L, 70_000L, 300_000L, 0).perWeekLater)
    }

    @Test
    fun moreThanFreeMoney_dipsIntoSavings() {
        // $1,000 free for the rest of the month (bills and goal already set aside), a $1,500 payment.
        assertEquals(50_000L, weighPayment(150_000L, 70_000L, 100_000L, 15).savingsDip)
    }

    @Test
    fun alreadyInTheRed_wholePaymentDipsIntoSavings() {
        // D25: free money is negative, so every peso of it comes out of the goal.
        val w = weighPayment(20_000L, leftThisWeek = -5_000L, freeMoneyNow = -40_000L, daysAfterThisWeek = 10)
        assertEquals(20_000L, w.overspend)
        assertEquals(20_000L, w.savingsDip)
    }

    // ---- dueDates ----

    private fun d(m: Int, day: Int) = LocalDate.of(2026, m, day)

    @Test
    fun dueDates_fromAnchorToToday() {
        val quincena = Schedule(ScheduleType.BIWEEKLY, anchorDate = d(11, 1))
        assertEquals(listOf(d(11, 15)), dueDates(quincena, null, d(11, 22)))
        assertEquals(listOf(d(11, 15), d(11, 30)), dueDates(quincena, null, d(11, 30)))
    }

    @Test
    fun dueDates_nothingBeforeAnchor() {
        val rent = Schedule(ScheduleType.START_OF_MONTH, anchorDate = d(11, 23))
        assertEquals(emptyList<LocalDate>(), dueDates(rent, null, d(11, 22)))
    }

    @Test
    fun dueDates_stopWhenCancelled() {
        val claude = Schedule(ScheduleType.DAY_OF_MONTH, anchorDate = d(10, 1), dayOfMonth = 10)
        // Cancelled on Nov 5: October's counts, November's doesn't.
        assertEquals(listOf(d(10, 10)), dueDates(claude, stoppedOn = d(11, 5), today = d(11, 22)))
    }
}
