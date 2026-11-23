package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.LedgerEntry
import com.diegodavid.supersave.engine.BudgetTrend
import kotlin.math.absoluteValue

/**
 * The main screen: how much is left this week, the D6 message, the D13 badges,
 * and this week's expenses. Replaces the old debug screen. (UI by Claude, Nivel 2.)
 */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onWeigh: () -> Unit,
    onCorrect: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data by vm.home.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<LedgerEntry?>(null) }
    val home = data ?: return
    val week = home.week

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_week, formatDate(week.week.start), formatDate(week.week.end)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSettings) { Text(stringResource(R.string.settings_title)) }
        }

        Text(stringResource(R.string.left_this_week), style = MaterialTheme.typography.titleMedium)
        Text(formatMoney(week.left), style = MaterialTheme.typography.displayMedium)
        Text(
            stringResource(R.string.home_budget_spent, formatMoney(week.budget), formatMoney(week.spent)),
            style = MaterialTheme.typography.bodyMedium,
        )

        // D6: compare with last week. Rewarding when up, neutral when down.
        when (week.trend) {
            BudgetTrend.UP -> MessageCard(stringResource(R.string.trend_up))
            BudgetTrend.DOWN -> MessageCard(stringResource(R.string.trend_down))
            else -> {}
        }
        // D13 heads-up badges.
        if (week.payday) MessageCard(stringResource(R.string.payday_badge))
        if (week.paymentsDue.isNotEmpty()) {
            MessageCard(stringResource(R.string.payment_due_badge, week.paymentsDue.joinToString()))
        }

        Button(onClick = onWeigh, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.weigh_button))
        }
        // D14: "Correct my balance" must be easy to find.
        OutlinedButton(onClick = onCorrect, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.correct_button, formatMoney(home.balance)))
        }

        SectionTitle(stringResource(R.string.home_expenses_title))
        if (home.expenses.isEmpty()) {
            Text(stringResource(R.string.home_expenses_empty), style = MaterialTheme.typography.bodyMedium)
        }
        home.expenses.forEach { e ->
            ListItem(
                headlineContent = { Text(e.note ?: stringResource(R.string.home_expense_no_note)) },
                supportingContent = { Text(formatDate(e.date)) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMoney(e.amount.absoluteValue))
                        TextButton(onClick = { deleting = e }) { Text(stringResource(R.string.setup_remove)) }
                    }
                },
            )
        }

        // TEMPORARY debug tools, until the app is done.
        HorizontalDivider(Modifier.padding(top = 16.dp))
        Row {
            TextButton(onClick = { vm.loadTestData() }) { Text(stringResource(R.string.debug_load)) }
            TextButton(onClick = { vm.clearAll() }) { Text(stringResource(R.string.debug_clear)) }
        }
    }

    deleting?.let { e ->
        ConfirmDialog(
            title = stringResource(R.string.home_delete_title),
            body = stringResource(R.string.home_delete_body, formatMoney(e.amount.absoluteValue)),
            confirm = stringResource(R.string.setup_remove),
            onConfirm = { vm.deleteExpense(e.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun MessageCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, Modifier.padding(16.dp))
    }
}
