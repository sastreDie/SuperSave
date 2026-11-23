package com.diegodavid.supersave

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * "What day is it?" for the whole app, screens and widgets. (Debug plumbing by Claude.)
 *
 * Normally it's the real date. For testing, the debug "Pretend today is…" button sets
 * a fake date, so weeks and month ends can be checked without changing the phone's
 * clock. The fake date is saved, so the widgets use it too, even after a restart.
 */
object AppClock {
    private const val PREFS = "debug"
    private const val KEY = "pretend_today"
    private var prefs: SharedPreferences? = null

    private val _pretend = MutableStateFlow<LocalDate?>(null)

    /** The fake date, or null when using the real one. Screens watch it to redraw. */
    val pretend: StateFlow<LocalDate?> = _pretend

    /** Called once by SuperSaveApp, before anything asks for the date. */
    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        _pretend.value = p.getString(KEY, null)?.let(LocalDate::parse)
    }

    fun today(): LocalDate = _pretend.value ?: LocalDate.now()

    /** null = go back to the real date. */
    fun setPretend(date: LocalDate?) {
        _pretend.value = date
        prefs?.edit()?.apply { if (date == null) remove(KEY) else putString(KEY, date.toString()) }?.apply()
    }
}
