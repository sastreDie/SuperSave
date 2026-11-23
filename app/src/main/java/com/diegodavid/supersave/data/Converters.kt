package com.diegodavid.supersave.data

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.YearMonth

/**
 * SQLite has no date type, so Room asks us how to turn dates into text and back.
 * ISO text ("2026-11-22", "2026-11") also sorts correctly, so date ranges work in SQL.
 */
class Converters {
    @TypeConverter fun localDateToText(date: LocalDate?): String? = date?.toString()
    @TypeConverter fun textToLocalDate(text: String?): LocalDate? = text?.let(LocalDate::parse)

    @TypeConverter fun yearMonthToText(month: YearMonth?): String? = month?.toString()
    @TypeConverter fun textToYearMonth(text: String?): YearMonth? = text?.let(YearMonth::parse)
}
