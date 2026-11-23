package com.diegodavid.supersave.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate


@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val weekStartDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val monthlySavingsGoal: Long,
    val defaultReviewChoice: ReviewChoice = ReviewChoice.KEEP,
    // Database v4: the day setup was finished. In that month the savings goal only
    // counts for the days from then on (D37). null = set up before v4: full goal.
    val startedOn: LocalDate? = null,
)
