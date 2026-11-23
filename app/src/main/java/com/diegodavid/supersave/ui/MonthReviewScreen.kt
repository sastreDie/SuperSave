package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.engine.monthCloseTransfer
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * The month review (D9, D21, D25): how much was saved, how the goal went, and what to
 * do with what was left over. The important number, "You saved", goes up when Keep it
 * is picked. (UI by Claude, Nivel 2.)
 */
@Composable
fun MonthReviewScreen(vm: MainViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val home by vm.home.collectAsStateWithLifecycle()
    val review = home?.pendingReview
    if (review == null) {
        // Nothing to review (already done, or still loading): go back to home.
        LaunchedEffect(home) { if (home != null) onDone() }
        return
    }

    var keep by rememberSaveable(review.month.toString()) { mutableStateOf(review.kept) }
    val surplus = review.surplus.coerceAtLeast(0)
    val goalSaved = monthCloseTransfer(review.savingsGoal, review.surplus, keep = false)
    val short = review.savingsGoal - goalSaved
    val savedTotal = monthCloseTransfer(review.savingsGoal, review.surplus, keep)
    val monthName = monthName(review.month)

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(stringResource(R.string.review_title, monthName))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(stringResource(R.string.review_you_saved), style = MaterialTheme.typography.titleMedium)
                    Text(formatMoney(savedTotal), style = MaterialTheme.typography.displayMedium)
                }
            }

            Text(stringResource(R.string.review_goal_line, formatMoney(goalSaved), formatMoney(review.savingsGoal)))
            // D25 in the D6 tone: informative, never scolding.
            Text(
                if (short > 0) stringResource(R.string.review_short, formatMoney(short))
                else stringResource(R.string.review_goal_met),
                style = MaterialTheme.typography.bodyMedium,
            )

            if (surplus > 0) {
                SectionTitle(stringResource(R.string.review_leftover, formatMoney(surplus)))
                Text(stringResource(R.string.extra_question))
                Column(Modifier.selectableGroup()) {
                    RadioRow(stringResource(R.string.review_keep), keep) { keep = true }
                    RadioRow(stringResource(R.string.review_spend_now), !keep) { keep = false }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.reviewMonth(review.month, keep); onDone() }) {
                Text(stringResource(R.string.review_done))
            }
        }
    }
}

/** "November 2026" / "noviembre 2026", in the phone's language. */
fun monthName(month: YearMonth): String =
    month.format(DateTimeFormatter.ofPattern("LLLL yyyy")).replaceFirstChar { it.titlecase() }
