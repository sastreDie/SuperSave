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
import com.diegodavid.supersave.R
import com.diegodavid.supersave.engine.Weighing

/**
 * Weigh the payment (D29): type what something costs, see how it fits before paying,
 * then register it as an expense or skip it. Nothing is saved until "Register".
 * (UI by Claude, Nivel 2.)
 */
@Composable
fun WeighScreen(vm: MainViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val amount = parseMoney(amountText)?.takeIf { it > 0 }

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
            weighing?.takeIf { it.amount == amount }?.let { WeighingCard(it) }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onDone) { Text(stringResource(R.string.weigh_skip)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.addExpense(amount!!, note.trim().ifBlank { null }); onDone() },
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
