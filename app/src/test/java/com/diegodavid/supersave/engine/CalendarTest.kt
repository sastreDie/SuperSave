package com.diegodavid.supersave.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarTest {

    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun dateRange_countsBothEnds() {
        assertEquals(7, DateRange(d(2026, 11, 16), d(2026, 11, 22)).days)
        assertEquals(1, DateRange(d(2026, 11, 30), d(2026, 11, 30)).days)
    }

    @Test
    fun dateRange_contains() {
        val range = DateRange(d(2026, 11, 16), d(2026, 11, 22))
        assertTrue(d(2026, 11, 16) in range)
        assertTrue(d(2026, 11, 22) in range)
        assertFalse(d(2026, 11, 23) in range)
    }

    @Test
    fun weekSegments_normalWeek_isOneSegment() {
        assertEquals(
            listOf(DateRange(d(2026, 11, 16), d(2026, 11, 22))),
            weekSegments(d(2026, 11, 16))
        )
    }

    @Test
    fun weekSegments_designDocExample_splitsAtApril1() {
        // D17: Mon Mar 30 to Sun Apr 5 -> 2 days of March, 5 days of April.
        val segments = weekSegments(d(2026, 3, 30))
        assertEquals(
            listOf(
                DateRange(d(2026, 3, 30), d(2026, 3, 31)),
                DateRange(d(2026, 4, 1), d(2026, 4, 5)),
            ),
            segments
        )
        assertEquals(listOf(2, 5), segments.map { it.days })
    }

    @Test
    fun weekSegments_sundayWeek_splits() {
        assertEquals(
            listOf(
                DateRange(d(2026, 11, 29), d(2026, 11, 30)),
                DateRange(d(2026, 12, 1), d(2026, 12, 5)),
            ),
            weekSegments(d(2026, 11, 29))
        )
    }

    @Test
    fun weekSegments_weekStartingOnThe1st_isOneSegment() {
        assertEquals(
            listOf(DateRange(d(2027, 2, 1), d(2027, 2, 7))),
            weekSegments(d(2027, 2, 1))
        )
    }

    @Test
    fun weekSegments_crossesNewYear() {
        assertEquals(
            listOf(
                DateRange(d(2026, 12, 28), d(2026, 12, 31)),
                DateRange(d(2027, 1, 1), d(2027, 1, 3)),
            ),
            weekSegments(d(2026, 12, 28))
        )
    }
}
