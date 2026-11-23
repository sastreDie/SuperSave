package com.diegodavid.supersave.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Tests for reading money the user types (Claude, Nivel 2). Results are in cents. */
class FormatTest {
    @Test
    fun parsesPlainAndFormattedAmounts() {
        assertEquals(500_000L, parseMoney("5000"))
        assertEquals(123_450L, parseMoney("1,234.5"))
        assertEquals(123_456L, parseMoney("$1,234.56"))
        assertEquals(5L, parseMoney("0.05"))
        assertEquals(0L, parseMoney("0"))
    }

    @Test
    fun parsesNegativeBalances() {
        assertEquals(-30_000L, parseMoney("-300"))
    }

    @Test
    fun rejectsNonsense() {
        assertNull(parseMoney(""))
        assertNull(parseMoney("abc"))
        assertNull(parseMoney("12.345"))   // fractions of a cent
        assertNull(parseMoney("1.2.3"))
    }

    @Test
    fun roundTripsWithFormatMoney() {
        assertEquals(123_456L, parseMoney(formatMoney(123_456L)))
        assertEquals(-23_334L, parseMoney(formatMoney(-23_334L)))
    }
}
