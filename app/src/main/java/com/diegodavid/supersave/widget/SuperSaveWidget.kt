package com.diegodavid.supersave.widget

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.BudgetRepository
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.engine.WeekSummary
import com.diegodavid.supersave.ui.formatMoney
import java.time.LocalDate

class SuperSaveWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Phase 1: load
        val db = SuperSaveDatabase.get(context)
        val repository = BudgetRepository(db)
        val firstSummary = repository.currentWeek(LocalDate.now())

        // Phase 2: layout
        provideContent {
            // Glance keeps this widget "session" alive for a while after each update, and
            // while it's alive, updateAll() only redraws: provideGlance() above doesn't run
            // again. So the data must be reloaded in here. produceState starts with
            // firstSummary, then loads fresh numbers every time a table changes.
            // (Fix by Claude: the widget used to keep showing the old number.)
            val summary by produceState<WeekSummary?>(firstSummary) {
                db.invalidationTracker
                    .createFlow("ledger", "settings", "fixed_payments", "recurring_incomes")
                    .collect { value = repository.currentWeek(LocalDate.now()) }
            }

            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                ) {
                    Text(
                        text = context.getString(R.string.left_this_week),
                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                    )
                    val s = summary
                    if (s == null) {
                        // Explicit size: without it, Glance can reuse the 28sp style of the
                        // money text from the previous draw, and this message gets cut off.
                        Text(
                            text = context.getString(R.string.widget_no_data),
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp)
                        )
                    } else {
                        Text(
                            text = formatMoney(s.left),
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Starts the widget again when no session is running (SuperSaveApp calls this after
 * every data change). While a session is running, the produceState above already
 * picks up the change.
 */
suspend fun refreshWidget(context: Context) {
    SuperSaveWidget().updateAll(context)
}
