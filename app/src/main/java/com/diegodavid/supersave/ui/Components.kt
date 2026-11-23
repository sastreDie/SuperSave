package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diegodavid.supersave.R

// Small pieces shared by several screens. (UI scaffolding by Claude, Nivel 2.)

/** A screen title with an explanation under it. */
@Composable
fun ScreenTitle(title: String, body: String? = null) {
    Text(title, style = MaterialTheme.typography.headlineSmall)
    if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

/** A text field for money: "$" prefix, decimal keyboard, error if it isn't an amount. */
@Composable
fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String, isError: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        prefix = { Text("$") },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.setup_money_error)) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A radio button with its label; the whole row is tappable. */
@Composable
fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}

/**
 * One number on the dashboard: a small label over the amount (D32).
 * Negative amounts use the error color: informative, not scolding (D6, D25).
 */
@Composable
fun StatTile(label: String, amount: Long, modifier: Modifier = Modifier, small: Boolean = false) {
    Card(modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = if (small) 8.dp else 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(
                formatMoney(amount),
                style = if (small) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                color = if (amount < 0) MaterialTheme.colorScheme.error else Color.Unspecified,
                maxLines = 1,
            )
        }
    }
}

/** "Are you sure?" before something that can't be undone. */
@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
