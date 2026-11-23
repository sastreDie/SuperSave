package com.diegodavid.supersave.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek


@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey val id: Int = 1,
    val weekStartDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val monthlySavingsGoal: Long,
    val defaultReviewChoice: ReviewChoice = ReviewChoice.KEEP

)