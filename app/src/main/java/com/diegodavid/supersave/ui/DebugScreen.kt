package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.BudgetRepository
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.data.TestData
import com.diegodavid.supersave.engine.BudgetTrend
import com.diegodavid.supersave.engine.WeekSummary
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * TEMPORARY screen (Claude, Nivel 2): shows the raw numbers so we can check that the
 * database and the engine work together. The real home screen comes later.
 */
@Composable
fun DebugScreen(modifier: Modifier = Modifier) {
    val db = SuperSaveDatabase.get(LocalContext.current)
    val repository = remember { BudgetRepository(db) }
    // State: when `summary` changes, Compose redraws the parts that read it.
    var summary by remember { mutableStateOf<WeekSummary?>(null) }
    val scope = rememberCoroutineScope()

    // Runs once when the screen appears. Database calls are `suspend`, so they need a coroutine.
    suspend fun refresh() { summary = repository.currentWeek(LocalDate.now()) }
    LaunchedEffect(Unit) { refresh() }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.debug_title), style = MaterialTheme.typography.titleLarge)

        val s = summary
        if (s == null) {
            Text(stringResource(R.string.debug_not_set_up))
        } else {
            Text(stringResource(R.string.debug_week, s.week.start.toString(), s.week.end.toString()))
            Text(stringResource(R.string.left_this_week))
            Text(formatMoney(s.left), style = MaterialTheme.typography.displayMedium)
            Text(stringResource(R.string.debug_week_budget, formatMoney(s.budget)))
            Text(stringResource(R.string.debug_spent, formatMoney(s.spent)))
            s.lastWeekBudget?.let { Text(stringResource(R.string.debug_last_week, formatMoney(it))) }
            when (s.trend) {
                BudgetTrend.UP -> Text(stringResource(R.string.trend_up))
                BudgetTrend.DOWN -> Text(stringResource(R.string.trend_down))
                else -> {}
            }
            if (s.payday) Text(stringResource(R.string.payday_badge))
            if (s.paymentsDue.isNotEmpty()) {
                Text(stringResource(R.string.payment_due_badge, s.paymentsDue.joinToString()))
            }
        }

        Button(onClick = { scope.launch { TestData.load(db); refresh() } }) {
            Text(stringResource(R.string.debug_load))
        }
        Button(onClick = { scope.launch { repository.addExpense(LocalDate.now(), 10_000L); refresh() } }) {
            Text(stringResource(R.string.debug_add_expense))
        }
        OutlinedButton(onClick = { scope.launch { TestData.clear(db); refresh() } }) {
            Text(stringResource(R.string.debug_clear))
        }
    }
}
