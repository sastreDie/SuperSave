package com.diegodavid.supersave.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Month close (D33). Tests by Claude. Amounts in cents. */
class MonthCloseTest {

    @Test
    fun keepIt_savesGoalAndSurplus() {
        // $2,000 goal, $300 left of the last week: $2,300 to savings.
        assertEquals(230_000L, monthCloseTransfer(goal = 200_000L, leftover = 30_000L, keep = true))
    }

    @Test
    fun spendIt_savesOnlyTheGoal() {
        // The $300 stays spendable and grows next month's budget.
        assertEquals(200_000L, monthCloseTransfer(200_000L, 30_000L, keep = false))
    }

    @Test
    fun lastWeekOverspent_comesOutOfTheGoal() {
        // $500 over in the last week: only $1,500 of the goal is saved.
        assertEquals(150_000L, monthCloseTransfer(200_000L, -50_000L, keep = true))
    }

    @Test
    fun hugeOverspend_neverTakesFromSavings() {
        assertEquals(0L, monthCloseTransfer(200_000L, -900_000L, keep = true))
    }

    @Test
    fun noGoal_keepStillSavesTheSurplus() {
        assertEquals(30_000L, monthCloseTransfer(0L, 30_000L, keep = true))
        assertEquals(0L, monthCloseTransfer(0L, 30_000L, keep = false))
    }
}
