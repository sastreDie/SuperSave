package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.remember
import com.diegodavid.supersave.AppClock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.diegodavid.supersave.R
import com.diegodavid.supersave.engine.Weighing

/**
 * "Check & record an expense" (D29, called Weigh the payment in the design): type what
 * something costs, see how it fits before paying, then record it or skip it.
 * Nothing is saved until "Record expense".
 * (UI by Claude, Nivel 2.)
 */
@Composable
fun WeighScreen(vm: MainViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val amount = parseMoney(amountText)?.takeIf { it > 0 }
    val home by vm.home.collectAsStateWithLifecycle()
    // D35: today by default; any earlier day of this month can be picked.
    val today = remember { AppClock.today() }
    var date by rememberSaveable { mutableStateOf(today) }
    // An expense in an earlier week doesn't touch this week's money, only the month's.
    val pastWeek = home?.let { date < it.week.week.start } ?: false

    // Recalculated every time the amount changes. Reads the database, so it runs in a coroutine.
    val weighing by produceState<Weighing?>(null, amount) {
        value = amount?.let { vm.weigh(it) }
    }

    Column(modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(stringResource(R.string.weigh_title), stringResource(R.string.weigh_body))

            // Mini dashboard (D32): what matters, after paying the amount typed so far.
            home?.let { h ->
                val cost = amount ?: 0L
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(stringResource(R.string.stat_week_short), h.week.left - if (pastWeek) 0 else cost, Modifier.weight(1f), small = true)
                    StatTile(stringResource(R.string.stat_month), h.leftThisMonth - cost, Modifier.weight(1f), small = true)
                    StatTile(stringResource(R.string.stat_balance), h.balance - cost, Modifier.weight(1f), small = true)
                }
                if (amount != null) {
                    Text(stringResource(R.string.weigh_after_note), style = MaterialTheme.typography.bodySmall)
                }
            }

            MoneyField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.weigh_amount),
                isError = amountText.isNotBlank() && amount == null,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.weigh_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            ExpenseDateButton(date, today, onPick = { date = it })
            if (pastWeek) {
                // The weighing compares with this week, so for an earlier week it would mislead.
                Text(stringResource(R.string.weigh_past_week), style = MaterialTheme.typography.bodyMedium)
            } else {
                weighing?.takeIf { it.amount == amount }?.let { WeighingCard(it) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDone) { Text(stringResource(R.string.weigh_skip)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.addExpense(amount!!, note.trim().ifBlank { null }, date); onDone() },
                enabled = amount != null,
            ) { Text(stringResource(R.string.weigh_register)) }
        }
    }
}

/** The answer, in plain words. Informative, never scolding (D6). */
@Composable
private fun WeighingCard(w: Weighing) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (w.fitsThisWeek) {
                Text(stringResource(R.string.weigh_fits, formatMoney(w.leftAfter)), style = MaterialTheme.typography.titleMedium)
            } else {
                Text(stringResource(R.string.weigh_over, formatMoney(w.overspend)), style = MaterialTheme.typography.titleMedium)
                val perWeek = w.perWeekLater
                if (perWeek != null) {
                    Text(stringResource(R.string.weigh_spread, formatMoney(perWeek)))
                } else {
                    Text(stringResource(R.string.weigh_last_week))
                }
            }
            if (w.savingsDip > 0) {
                Text(stringResource(R.string.weigh_savings_dip, formatMoney(w.savingsDip)))
            }
        }
    }
}

/**
 * "Date: Today" button with a date picker (D35). Only days of this month up to today:
 * earlier months are closed and their savings already moved (D33).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDateButton(date: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }) {
        Text(
            if (date == today) stringResource(R.string.weigh_date_today)
            else stringResource(R.string.weigh_date, formatDate(date))
        )
    }
    if (!picking) return

    fun LocalDate.millis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val first = today.withDayOfMonth(1).millis()
    val last = today.millis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.millis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in first..last
            override fun isSelectableYear(year: Int) = year == today.year
        },
    )
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                picking = false
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } },
    ) { DatePicker(state) }
}
