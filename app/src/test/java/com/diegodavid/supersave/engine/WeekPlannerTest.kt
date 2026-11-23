package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.FixedPayment
import com.diegodavid.supersave.data.RecurringIncome
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekPlannerTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val longAgo = d(2026, 1, 1)

    private val rent = FixedPayment(name = "Rent", amount = 300_000L,
        schedule = Schedule(ScheduleType.START_OF_MONTH, longAgo))
    private val claude = FixedPayment(name = "Claude", amount = 39_900L,
        schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 10))
    private val internet = FixedPayment(name = "Internet", amount = 50_000L,
        schedule = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 28))
    private val payments = listOf(rent, claude, internet)

    private val salary = RecurringIncome(name = "Salary", amount = 600_000L,
        schedule = Schedule(ScheduleType.BIWEEKLY, longAgo))

    @Test
    fun freeze_normalWeek() {
        // Mon Nov 16: $5,651 in the account, only internet ($500) still due, goal $2,000.
        // Free = 3,151. 15 days left -> 3,151 × 7 ÷ 15 = 1,470.466... -> $1,470.46.
        val week = DateRange(d(2026, 11, 16), d(2026, 11, 22))
        assertEquals(147_046L, freezeSegmentBudget(week, 565_100L, 0L, payments, 200_000L))
    }

    @Test
    fun freeze_aprilPartOfSplitWeek_reservesThatMonthsPayments() {
        // Dec 1-6 (the second part of Mon Nov 30's week): all 3 payments are still due in December.
        // 10,000 - 3,899 - 2,000 = 4,101 free; 4,101 × 6 ÷ 31 = 793.74... -> $793.74.
        val segment = DateRange(d(2026, 12, 1), d(2026, 12, 6))
        assertEquals(79_374L, freezeSegmentBudget(segment, 1_000_000L, 0L, payments, 200_000L))
    }

    @Test
    fun freeze_savedMoneyIsNotSpendable() {
        // D24: $2,000 kept from last month sits in the account but isn't free money.
        val week = DateRange(d(2026, 11, 16), d(2026, 11, 22))
        assertEquals(
            freezeSegmentBudget(week, 565_100L, 0L, payments, 200_000L),
            freezeSegmentBudget(week, 765_100L, 200_000L, payments, 200_000L),
        )
    }

    @Test
    fun payday_quincenaWeek() {
        assertTrue(paydayIn(listOf(salary), DateRange(d(2026, 11, 9), d(2026, 11, 15))))
        assertFalse(paydayIn(listOf(salary), DateRange(d(2026, 11, 16), d(2026, 11, 22))))
    }

    @Test
    fun payday_endedIncomeDoesNotCount() {
        val ended = salary.copy(endedOn = d(2026, 11, 1))
        assertFalse(paydayIn(listOf(ended), DateRange(d(2026, 11, 9), d(2026, 11, 15))))
    }

    @Test
    fun paymentsDue_namesForTheBadge() {
        assertEquals(listOf("Claude"), paymentNamesDueIn(payments, DateRange(d(2026, 11, 9), d(2026, 11, 15))))
        assertEquals(emptyList<String>(), paymentNamesDueIn(payments, DateRange(d(2026, 11, 16), d(2026, 11, 22))))
    }
}
