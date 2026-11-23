package com.diegodavid.supersave.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.YearMonth

/** How a month went, and what the user chose to do with the surplus (D9, D21). */
@Entity(tableName = "month_reviews")
data class MonthReview(
    @PrimaryKey val month: YearMonth,
    val savingsGoal: Long,
    val surplus: Long,
    // null = not reviewed yet; Settings.defaultReviewChoice applies.
    val choice: ReviewChoice? = null
)
