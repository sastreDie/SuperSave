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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.ReviewChoice

/**
 * Surprise money (D7, D31): money that arrived outside any schedule. The user picks
 * whether it goes to savings or to this month's spending. (UI by Claude, Nivel 2.)
 */
@Composable
fun SurpriseMoneyScreen(vm: MainViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var amountText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    // null = not chosen yet: the default from Settings (D21) applies.
    var save by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val saveIt = save ?: (settings?.defaultReviewChoice != ReviewChoice.SPEND)
    val amount = parseMoney(amountText)?.takeIf { it > 0 }

    Column(modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(stringResource(R.string.surprise_title), stringResource(R.string.surprise_body))
            MoneyField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.surprise_amount),
                isError = amountText.isNotBlank() && amount == null,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.surprise_note)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionTitle(stringResource(R.string.surprise_question))
            Column(Modifier.selectableGroup()) {
                RadioRow(stringResource(R.string.surprise_save), saveIt) { save = true }
                RadioRow(stringResource(R.string.surprise_spend), !saveIt) { save = false }
            }
            Text(
                stringResource(if (saveIt) R.string.surprise_save_note else R.string.surprise_spend_note),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = onDone) { Text(stringResource(android.R.string.cancel)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.addSurpriseMoney(amount!!, note.trim().ifBlank { null }, saveIt); onDone() },
                enabled = amount != null,
            ) { Text(stringResource(R.string.surprise_add)) }
        }
    }
}
