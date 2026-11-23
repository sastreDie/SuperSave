package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.FixedPayment
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for the money logic (tests by Claude; the functions are Diego's).
 * All amounts are in CENTS: ₱8,000.00 = 800_000. The underscores are just
 * for reading, like commas: Kotlin ignores them.
 */
class MoneyTest {

    // ---- freeMoney = spendable - payments still due this month - savings goal ----

    @Test
    fun freeMoney_designDocExample() {
        // ₱8,000 in the account, rent ₱3,000 still due, savings goal ₱2,000 -> ₱3,000 free.
        assertEquals(
            300_000L,
            freeMoney(spendable = 800_000L, upcomingPayments = listOf(300_000L), savingsGoal = 200_000L)
        )
    }

    @Test
    fun freeMoney_severalPayments() {
        // Rent ₱3,000 + Claude ₱399 + internet ₱500 = ₱3,899 reserved.
        // 8,000 - 3,899 - 2,000 = ₱2,101
        assertEquals(
            210_100L,
            freeMoney(
                spendable = 800_000L,
                upcomingPayments = listOf(300_000L, 39_900L, 50_000L),
                savingsGoal = 200_000L
            )
        )
    }

    @Test
    fun freeMoney_noPaymentsLeft() {
        // Late in the month everything is already paid: only the savings goal is reserved.
        assertEquals(
            600_000L,
            freeMoney(spendable = 800_000L, upcomingPayments = emptyList(), savingsGoal = 200_000L)
        )
    }

    @Test
    fun freeMoney_canBeNegative() {
        // D25: ₱4,000 in the account, rent ₱3,000, goal ₱2,000 -> ₱1,000 short.
        // We keep the negative number so the app can say how far from the goal you are.
        assertEquals(
            -100_000L,
            freeMoney(spendable = 400_000L, upcomingPayments = listOf(300_000L), savingsGoal = 200_000L)
        )
    }

    // ---- segmentBudget ----

    @Test
    fun segmentBudget_fullWeek_designDocExample() {
        // ₱3,000 free, 7-day week, 30 days left -> ₱700.
        assertEquals(70_000L, segmentBudget(300_000L, 7, 30))
    }

    @Test
    fun segmentBudget_nextWeekAfterSaving() {
        // ₱2,500 free, 23 days left -> 2,500 × 7 ÷ 23 = ₱760.869... -> ₱760.86 (rounded down).
        assertEquals(76_086L, segmentBudget(250_000L, 7, 23))
    }

    @Test
    fun segmentBudget_shortSegmentAtMonthEnd() {
        // Mon Nov 30 alone: 1 day left in the month -> all the free money.
        assertEquals(50_000L, segmentBudget(50_000L, 1, 1))
    }

    @Test
    fun segmentBudget_negativeRoundsDown() {
        // D25 + D26: -₱1,000 × 7 ÷ 30 = -₱233.333... -> -₱233.34, never -₱233.33.
        assertEquals(-23_334L, segmentBudget(-100_000L, 7, 30))
    }

    // ---- weekBudget / leftThisWeek ----

    @Test
    fun weekBudget_splitWeek() {
        // Mar 30-31 from March + Apr 1-5 from April.
        assertEquals(95_000L, weekBudget(listOf(20_000L, 75_000L)))
    }

    @Test
    fun leftThisWeek_afterSpending() {
        assertEquals(45_000L, leftThisWeek(weekBudget = 70_000L, spent = 25_000L))
        assertEquals(-5_000L, leftThisWeek(weekBudget = 70_000L, spent = 75_000L))
    }

    // ---- correctionForSegment (D18) ----

    @Test
    fun correction_foundMoney() {
        // Bank shows ₱300 more than the app, 3 days left in the week, 15 in the month -> +₱60.
        assertEquals(6_000L, correctionForSegment(30_000L, 3, 15))
    }

    @Test
    fun correction_missingMoney() {
        // App thought you had ₱100 more: -100 × 7 ÷ 30 = -₱23.333... -> -₱23.34.
        assertEquals(-2_334L, correctionForSegment(-10_000L, 7, 30))
    }

    // ---- paymentsDueIn ----

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val longAgo = d(2026, 1, 1)

    @Test
    fun paymentsDue_onlyThoseLeftThisMonth() {
        val rent = FixedPayment(name = "Rent", amount = 300_000L,
            schedule = Schedule(ScheduleType.START_OF_MONTH, longAgo))
        val claude = FixedPayment(name = "Claude", amount = 39_900L,
            schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 10))
        // From Nov 5: rent (Nov 1) is already paid, Claude (Nov 10) is still due.
        assertEquals(listOf(39_900L), paymentsDueIn(listOf(rent, claude), restOfMonth(d(2026, 11, 5))))
    }

    @Test
    fun paymentsDue_quincenaCountsTwice() {
        val gym = FixedPayment(name = "Gym", amount = 25_000L,
            schedule = Schedule(ScheduleType.BIWEEKLY, longAgo))
        assertEquals(listOf(25_000L, 25_000L), paymentsDueIn(listOf(gym), restOfMonth(d(2026, 11, 1))))
    }

    @Test
    fun paymentsDue_cancelledStopsCounting() {
        // Cancelled on Nov 8: the Nov 10 charge is no longer expected (D15).
        val claude = FixedPayment(name = "Claude", amount = 39_900L,
            schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 10),
            cancelledOn = d(2026, 11, 8))
        assertEquals(emptyList<Long>(), paymentsDueIn(listOf(claude), restOfMonth(d(2026, 11, 1))))
    }

    // ---- budgetTrend (D6) ----

    @Test
    fun budgetTrend_picksTheMessage() {
        assertEquals(BudgetTrend.UP, budgetTrend(76_086L, 70_000L))
        assertEquals(BudgetTrend.DOWN, budgetTrend(60_000L, 70_000L))
        assertEquals(BudgetTrend.SAME, budgetTrend(70_000L, 70_000L))
    }
}
