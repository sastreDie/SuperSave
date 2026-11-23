package com.diegodavid.supersave

import android.app.Application
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.widget.refreshWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Runs once when the app's process starts, before any screen or the widget.
 * (Plumbing by Claude, Nivel 2.)
 *
 * It keeps the home-screen widgets in sync: Room tells us whenever one of these
 * tables changes, and we redraw the widgets. So every write (an expense, setup,
 * test data, corrections, payments) updates them without each screen having to
 * remember to call refreshWidget(). Changing the debug "pretend today" date does too.
 */
class SuperSaveApp : Application() {
    // Lives as long as the process. SupervisorJob: one failed refresh doesn't stop the rest.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        AppClock.init(this)
        val db = SuperSaveDatabase.get(this)
        appScope.launch {
            db.invalidationTracker
                // Not "segments" or "month_reviews": the widget itself writes those while
                // drawing, and watching them would make it redraw itself again.
                .createFlow(
                    "ledger", "settings", "fixed_payments", "recurring_incomes",
                    emitInitialState = false,
                )
                // If several changes arrive while a redraw is running, redraw once more, not once each.
                .conflate()
                .collect { refreshWidget(this@SuperSaveApp) }
        }
        appScope.launch {
            // drop(1): skip the current value, only react to changes.
            AppClock.pretend.drop(1).collect { refreshWidget(this@SuperSaveApp) }
        }
    }
}
