package com.diegodavid.supersave.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "segments")
data class Segment(
    @PrimaryKey val start: LocalDate,
    val end: LocalDate,
    val budget: Long
)