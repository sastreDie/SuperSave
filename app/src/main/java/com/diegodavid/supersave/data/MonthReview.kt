package com.diegodavid.supersave.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.YearMonth

/** How a month went, and what was done with the surplus (D9, D21, D33). */
@Entity(tableName = "month_reviews")
data class MonthReview(
    @PrimaryKey val month: YearMonth,
    val savingsGoal: Long,
    /**
     * What was left of the month's last segment budget (D33). Negative when the last
     * week was overspent; that part came out of the goal.
     */
    val surplus: Long,
    /** What was done with a positive surplus. Rows from before database v3 may be null = KEEP. */
    val choice: ReviewChoice? = null,
    /** Database v3: false until the user has seen the review screen (D9). */
    @ColumnInfo(defaultValue = "0") val reviewed: Boolean = false,
) {
    val kept: Boolean get() = choice != ReviewChoice.SPEND
}
