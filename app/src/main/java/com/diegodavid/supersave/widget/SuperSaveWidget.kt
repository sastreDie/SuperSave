package com.diegodavid.supersave.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.diegodavid.supersave.AppClock
import com.diegodavid.supersave.MainActivity
import com.diegodavid.supersave.R
import com.diegodavid.supersave.Screen
import com.diegodavid.supersave.data.BudgetRepository
import com.diegodavid.supersave.data.HomeData
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.ui.formatMoney

// The sizes the summary widget has a layout for (D32). On a Pixel a cell is about
// 95 dp wide and 100 dp tall. Glance picks the biggest layout that fits.
private val WEEK_ONLY = DpSize(100.dp, 48.dp)        // 2×1: just this week
private val WEEK_AND_MONTH = DpSize(260.dp, 48.dp)   // 3×1, 4×1: this week + month
private val STACKED = DpSize(100.dp, 150.dp)         // 2×2: week, month, balance
private val WIDE = DpSize(260.dp, 150.dp)            // 3×2, 4×2: week + three tiles
private val FULL = DpSize(260.dp, 260.dp)            // 3×3, 4×3: + budget, spent, badge

/**
 * The summary widget (D32): a small home screen that shows more the bigger it is,
 * from 2×1 (only this week) to 4×3. Tapping it opens the app.
 */
class SuperSaveWidget : GlanceAppWidget() {

    // Responsive: Glance draws one layout per size, and the launcher shows the one that fits.
    override val sizeMode = SizeMode.Responsive(setOf(WEEK_ONLY, WEEK_AND_MONTH, STACKED, WIDE, FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Phase 1: load
        val repository = BudgetRepository(SuperSaveDatabase.get(context))
        val first = repository.home(AppClock.today())

        // Phase 2: layout
        provideContent {
            val home = liveHome(repository, first)
            val size = LocalSize.current
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .cornerRadius(16.dp)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .clickable(actionStartActivity(MainActivity.intentFor(context, Screen.HOME))),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (home == null) {
                        Label(context.getString(R.string.left_this_week))
                        NotSetUp(context)
                    } else when (size) {
                        WEEK_ONLY -> Week(context, home, 22.sp)
                        WEEK_AND_MONTH -> Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(GlanceModifier.defaultWeight()) { Week(context, home, 22.sp) }
                            Spacer(GlanceModifier.width(8.dp))
                            Stat(context.getString(R.string.stat_month), home.leftThisMonth, GlanceModifier.defaultWeight())
                        }
                        STACKED -> {
                            Week(context, home, 24.sp)
                            Spacer(GlanceModifier.height(6.dp))
                            Stat(context.getString(R.string.stat_month), home.leftThisMonth, GlanceModifier)
                            Stat(context.getString(R.string.stat_balance), home.balance, GlanceModifier)
                        }
                        else -> {
                            Week(context, home, 30.sp)
                            if (size == FULL) {
                                Text(
                                    context.getString(R.string.home_budget_spent, formatMoney(home.week.budget), formatMoney(home.week.spent)),
                                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                                )
                            }
                            Spacer(GlanceModifier.height(8.dp))
                            Row(GlanceModifier.fillMaxWidth()) {
                                Stat(context.getString(R.string.stat_month), home.leftThisMonth, GlanceModifier.defaultWeight())
                                Stat(context.getString(R.string.stat_balance), home.balance, GlanceModifier.defaultWeight())
                                Stat(context.getString(R.string.stat_saved), home.saved, GlanceModifier.defaultWeight())
                            }
                            if (size == FULL) {
                                headsUp(context, home)?.let { message ->
                                    Spacer(GlanceModifier.height(8.dp))
                                    Text(
                                        message,
                                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                                        maxLines = 2,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Quick-expense widget sizes: 1×1 is a big "+" with what's left; 2×1 and up has the label.
private val QUICK_ICON = DpSize(40.dp, 40.dp)
private val QUICK_FULL = DpSize(150.dp, 48.dp)

/**
 * The quick-expense widget: a "+ Record expense" button with what's left this week.
 * Tapping it opens the app straight on "Check & record an expense".
 */
class QuickExpenseWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(QUICK_ICON, QUICK_FULL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = BudgetRepository(SuperSaveDatabase.get(context))
        val first = repository.home(AppClock.today())

        provideContent {
            val home = liveHome(repository, first)
            val icon = LocalSize.current == QUICK_ICON
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.primaryContainer)
                        .cornerRadius(16.dp)
                        .padding(if (icon) 6.dp else 12.dp)
                        .clickable(actionStartActivity(MainActivity.intentFor(context, Screen.EXPENSE))),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = if (icon) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    if (icon) {
                        // 1×1: no room for words, just the "+" and what's left this week.
                        Text(
                            text = "+",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (home != null) {
                            Text(
                                text = formatMoney(home.week.left),
                                style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 12.sp),
                                maxLines = 1,
                            )
                        }
                        return@Column
                    }
                    Text(
                        text = context.getString(R.string.quick_widget_action),
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimaryContainer,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    if (home != null) {
                        Text(
                            text = context.getString(R.string.quick_widget_left, formatMoney(home.week.left)),
                            style = TextStyle(color = GlanceTheme.colors.onPrimaryContainer, fontSize = 13.sp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Glance keeps a widget "session" alive for a while after each update, and while it's
 * alive, updateAll() only redraws: provideGlance() doesn't run again. So the data is
 * reloaded in here, every time a table (or the debug date) changes. Starts with [first].
 */
@Composable
private fun liveHome(repository: BudgetRepository, first: HomeData?): HomeData? {
    val home by produceState(first) {
        repository.observeHome().collect { value = it }
    }
    return home
}

/** "Left this week" and the big number. */
@Composable
private fun Week(context: Context, home: HomeData, size: TextUnit) {
    Label(context.getString(R.string.left_this_week))
    Text(
        text = formatMoney(home.week.left),
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = size, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

/** The D13 badges, payday first. (The D6 messages are out of the first version.) */
private fun headsUp(context: Context, home: HomeData): String? = when {
    home.week.payday -> context.getString(R.string.payday_badge)
    home.week.paymentsDue.isNotEmpty() ->
        context.getString(R.string.payment_due_badge, home.week.paymentsDue.joinToString())
    else -> null
}

@Composable
private fun Label(text: String) {
    Text(text, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
}

@Composable
private fun Stat(label: String, amount: Long, modifier: GlanceModifier) {
    Column(modifier) {
        Text(label, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp), maxLines = 1)
        Text(
            formatMoney(amount),
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

@Composable
private fun NotSetUp(context: Context) {
    // Explicit size: without it, Glance can reuse the big style of the money text from
    // the previous draw, and this message gets cut off.
    Text(
        text = context.getString(R.string.widget_no_data),
        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp),
    )
}

/**
 * Starts the widgets again when no session is running (SuperSaveApp calls this after
 * every data change). While a session is running, liveHome() already picks up the change.
 */
suspend fun refreshWidget(context: Context) {
    SuperSaveWidget().updateAll(context)
    QuickExpenseWidget().updateAll(context)
}
