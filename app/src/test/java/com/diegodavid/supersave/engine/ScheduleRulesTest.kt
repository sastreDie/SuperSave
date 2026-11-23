package com.diegodavid.supersave.engine

import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleRulesTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)
    private val longAgo = d(2026, 1, 1)

    @Test
    fun endOfMonth() {
        val s = Schedule(ScheduleType.END_OF_MONTH, longAgo)
        assertTrue(occursOn(s, d(2026, 11, 30)))
        assertTrue(occursOn(s, d(2027, 2, 28)))
        assertFalse(occursOn(s, d(2026, 11, 29)))
    }

    @Test
    fun startOfMonth() {
        val s = Schedule(ScheduleType.START_OF_MONTH, longAgo)
        assertTrue(occursOn(s, d(2026, 12, 1)))
        assertFalse(occursOn(s, d(2026, 12, 2)))
    }

    @Test
    fun biweekly_quincena() {
        val s = Schedule(ScheduleType.BIWEEKLY, longAgo)
        assertEquals(
            listOf(d(2026, 11, 15), d(2026, 11, 30)),
            occurrencesIn(s, DateRange(d(2026, 11, 1), d(2026, 11, 30)))
        )
    }

    @Test
    fun everyNDays_countsFromAnchor() {
        val s = Schedule(ScheduleType.EVERY_N_DAYS, d(2026, 11, 6), everyNDays = 14)
        assertTrue(occursOn(s, d(2026, 11, 6)))
        assertTrue(occursOn(s, d(2026, 11, 20)))
        assertTrue(occursOn(s, d(2026, 12, 4)))
        assertFalse(occursOn(s, d(2026, 11, 13)))
    }

    @Test
    fun dayOfMonth_claudeSubscriptionOnThe10th() {
        val s = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 10)
        assertEquals(
            listOf(d(2026, 11, 10), d(2026, 12, 10)),
            occurrencesIn(s, DateRange(d(2026, 11, 1), d(2026, 12, 31)))
        )
    }

    @Test
    fun dayOfMonth_31st_fallsBackToLastDay() {
        val s = Schedule(ScheduleType.DAY_OF_MONTH, longAgo, dayOfMonth = 31)
        assertTrue(occursOn(s, d(2026, 11, 30)))   // November has 30 days
        assertTrue(occursOn(s, d(2027, 2, 28)))    // February 2027 has 28
        assertTrue(occursOn(s, d(2026, 12, 31)))
        assertFalse(occursOn(s, d(2026, 12, 30)))
    }

    @Test
    fun nothingBeforeAnchorDate() {
        // A rent added on Nov 20 must not count Nov 1 as a payment.
        val s = Schedule(ScheduleType.START_OF_MONTH, d(2026, 11, 20))
        assertFalse(occursOn(s, d(2026, 11, 1)))
        assertTrue(occursOn(s, d(2026, 12, 1)))
    }

    @Test
    fun missingSettings_neverOccur() {
        assertFalse(occursOn(Schedule(ScheduleType.EVERY_N_DAYS, longAgo), d(2026, 11, 22)))
        assertFalse(occursOn(Schedule(ScheduleType.DAY_OF_MONTH, longAgo), d(2026, 11, 22)))
    }
}
