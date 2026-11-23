package com.diegodavid.supersave.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Tests for the budget engine (written by Claude, Nivel 2: test harness).
 * Each test = one example from DESIGN.md turned into a check the computer runs.
 * Run: open this file in Android Studio and click the green arrow next to the class name.
 */
class BudgetMathTest {

    // ---- Step 1: daysLeftInMonth (today counts as a day left) ----

    @Test
    fun daysLeftInMonth_midMonth() {
        // Nov 22, 23, ..., 30 = 9 days
        assertEquals(9, daysLeftInMonth(LocalDate.of(2026, 11, 22)))
    }

    @Test
    fun daysLeftInMonth_firstDay() {
        assertEquals(30, daysLeftInMonth(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun daysLeftInMonth_lastDay() {
        // On the last day there is still today.
        assertEquals(1, daysLeftInMonth(LocalDate.of(2026, 11, 30)))
    }

    @Test
    fun daysLeftInMonth_31DayMonth() {
        assertEquals(1, daysLeftInMonth(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun daysLeftInMonth_leapFebruary() {
        // 2028 is a leap year: February has 29 days.
        assertEquals(29, daysLeftInMonth(LocalDate.of(2028, 2, 1)))
    }

    // ---- Step 2a: startOfWeek (the most recent firstDay on or before the date) ----

    @Test
    fun startOfWeek_mondayWeek_fromSunday() {
        // Sun Nov 22 is the LAST day of a Monday week, which started Mon Nov 16.
        assertEquals(
            LocalDate.of(2026, 11, 16),
            startOfWeek(LocalDate.of(2026, 11, 22), DayOfWeek.MONDAY)
        )
    }

    @Test
    fun startOfWeek_sundayWeek_isSameDay() {
        assertEquals(
            LocalDate.of(2026, 11, 22),
            startOfWeek(LocalDate.of(2026, 11, 22), DayOfWeek.SUNDAY)
        )
    }

    @Test
    fun startOfWeek_sundayWeek_fromWednesday() {
        assertEquals(
            LocalDate.of(2026, 11, 22),
            startOfWeek(LocalDate.of(2026, 11, 25), DayOfWeek.SUNDAY)
        )
    }

    @Test
    fun startOfWeek_mondayWeek_onMonday() {
        assertEquals(
            LocalDate.of(2026, 11, 16),
            startOfWeek(LocalDate.of(2026, 11, 16), DayOfWeek.MONDAY)
        )
    }

    @Test
    fun startOfWeek_crossesIntoPreviousYear() {
        // Fri Jan 1, 2027 -> the week started Mon Dec 28, 2026.
        assertEquals(
            LocalDate.of(2026, 12, 28),
            startOfWeek(LocalDate.of(2027, 1, 1), DayOfWeek.MONDAY)
        )
    }
}
