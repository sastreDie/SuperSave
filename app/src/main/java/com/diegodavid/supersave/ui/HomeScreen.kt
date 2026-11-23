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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R

/**
 * The main screen, as a dashboard (D32): this week's money in the biggest card, then
 * the month, the balance and savings; the D13 badges; the actions;
 * and the activity log (D34). (UI by Claude, Nivel 2.)
 */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onExpense: () -> Unit,
    onExtra: () -> Unit,
    onCorrect: () -> Unit,
    onReview: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val data by vm.home.collectAsStateWithLifecycle()
    val home = data ?: return
    val week = home.week

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PretendDateBanner()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.home_week, formatDate(week.week.start), formatDate(week.week.end)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSettings) { Text(stringResource(R.string.settings_title)) }
        }

        // The biggest number: what's left to spend this week (D3).
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(stringResource(R.string.left_this_week), style = MaterialTheme.typography.titleMedium)
                Text(
                    formatMoney(week.left),
                    style = MaterialTheme.typography.displayMedium,
                    color = if (week.left < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                )
                Text(
                    stringResource(R.string.home_budget_spent, formatMoney(week.budget), formatMoney(week.spent)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(stringResource(R.string.stat_month), home.leftThisMonth, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_balance), home.balance, Modifier.weight(1f))
            StatTile(stringResource(R.string.stat_saved), home.saved, Modifier.weight(1f))
        }
        Text(stringResource(R.string.home_goal_note, formatMoney(home.goal)), style = MaterialTheme.typography.bodySmall)

        // D9: a closed month waiting for its review.
        home.pendingReview?.let { review ->
            Card(
                onClick = onReview,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.review_card, monthName(review.month)), Modifier.padding(16.dp))
            }
        }
        // D25: how far from the month's plan, informative and never scolding (D6).
        if (home.leftThisMonth < 0) {
            MessageCard(stringResource(R.string.home_short, formatMoney(-home.leftThisMonth)))
        }
        // D13 heads-up badges. (The D6 "good job" messages are out of the first version.)
        if (week.payday) MessageCard(stringResource(R.string.payday_badge))
        if (week.paymentsDue.isNotEmpty()) {
            MessageCard(stringResource(R.string.payment_due_badge, week.paymentsDue.joinToString()))
        }

        Button(onClick = onExpense, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.expense_button))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onExtra, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.extra_button))
            }
            // D14: "Correct my balance" must be easy to find.
            OutlinedButton(onClick = onCorrect, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.correct_title))
            }
        }

        ActivityLog(vm)

        // TEMPORARY debug tools, until the app is done.
        HorizontalDivider(Modifier.padding(top = 16.dp))
        Row {
            TextButton(onClick = { vm.loadTestData() }) { Text(stringResource(R.string.debug_load)) }
            TextButton(onClick = { vm.clearAll() }) { Text(stringResource(R.string.debug_clear)) }
        }
        PretendDateButton()
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
