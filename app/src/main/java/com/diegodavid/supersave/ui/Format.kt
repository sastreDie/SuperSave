package com.diegodavid.supersave.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.absoluteValue

/** Cents -> "$1,470.46" / "-$233.34". Used by the app screens and the widget. */
fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = cents.absoluteValue
    return String.format(Locale.US, "%s$%,d.%02d", sign, abs / 100, abs % 100)
}

/**
 * What the user typed -> cents. "1,234.5" -> 123_450, "-300" -> -30_000.
 * Ignores "$", commas and spaces. Returns null if it isn't a number or has
 * more than 2 decimals, so the screen can show an error instead of guessing.
 */
fun parseMoney(text: String): Long? {
    val clean = text.replace("$", "").replace(",", "").replace(" ", "")
    val value = clean.toBigDecimalOrNull() ?: return null
    if (value.scale() > 2) return null
    return runCatching { value.movePointRight(2).longValueExact() }.getOrNull()
}

/** "Nov 22, 2026" in English, "22 nov 2026" in Spanish (follows the phone's language). */
fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
