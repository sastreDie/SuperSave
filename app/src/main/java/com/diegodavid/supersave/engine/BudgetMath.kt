package com.diegodavid.supersave.engine

import java.time.DayOfWeek
import java.time.LocalDate

fun daysLeftInMonth(today: LocalDate): Int {
    return today.lengthOfMonth() - today.dayOfMonth + 1
}

fun startOfWeek(date: LocalDate, firstDay: DayOfWeek): LocalDate {
    var currentDate = date
    while(currentDate.dayOfWeek != firstDay){
        currentDate = currentDate.minusDays(1)
    }
    return currentDate
}