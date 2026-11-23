package com.diegodavid.supersave.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.ActivityItem
import com.diegodavid.supersave.data.ActivityKind
import kotlin.math.absoluteValue

/**
 * The activity log on home (D34): expenses, incomes, extra money, fixed payments,
 * corrections and money moved to savings, newest first. Tap a line to adjust it.
 * (UI by Claude, Nivel 2.)
 */
@Composable
fun ActivityLog(vm: MainViewModel) {
    val items by vm.activity.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<ActivityItem?>(null) }

    SectionTitle(stringResource(R.string.activity_title))
    if (items.isEmpty()) {
        Text(stringResource(R.string.activity_empty), style = MaterialTheme.typography.bodyMedium)
    }
    items.forEach { item ->
        val e = item.entry
        // Moves to or from savings don't change the balance (D24), so they get no +/−.
        val sign = when {
            item.kind == ActivityKind.TO_SAVINGS -> ""
            e.amount > 0 -> "+"
            e.amount < 0 -> "−"
            else -> ""
        }
        ListItem(
            headlineContent = { Text(title(item)) },
            supportingContent = { Text("${formatDate(e.date)} · ${kindLabel(item)}") },
            trailingContent = {
                Text(
                    sign + formatMoney(e.amount.absoluteValue),
                    color = if (sign == "+") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            },
            modifier = if (item.editable) Modifier.clickable { editing = item } else Modifier,
        )
    }
    TextButton(onClick = { vm.showOlderActivity() }) { Text(stringResource(R.string.activity_show_older)) }

    editing?.let { item ->
        EditEntryDialog(
            item = item,
            onSave = { amount, note -> vm.updateEntry(item.entry.id, amount, note) },
            onRemove = { vm.removeEntry(item.entry.id) },
            onDismiss = { editing = null },
        )
    }
}

/** The big text of a line: what the user typed, else the income or payment name, else the kind. */
@Composable
private fun title(item: ActivityItem): String = item.entry.note ?: item.name ?: kindLabel(item)

@Composable
private fun kindLabel(item: ActivityItem): String = stringResource(
    when (item.kind) {
        ActivityKind.EXPENSE -> R.string.activity_expense
        ActivityKind.EXTRA_MONEY -> if (item.saved) R.string.activity_extra_saved else R.string.activity_extra
        ActivityKind.INCOME -> R.string.activity_income
        ActivityKind.FIXED_PAYMENT -> R.string.activity_payment
        ActivityKind.CORRECTION -> R.string.activity_correction
        // Negative = money taken back out of savings (a month review changed to Spend it).
        ActivityKind.TO_SAVINGS -> if (item.entry.amount < 0) R.string.activity_from_savings else R.string.activity_to_savings
    }
)

/**
 * Change the amount and note of a line. Expenses and extra money can also be removed.
 * A recurring income or payment can be set to $0 (e.g. the paycheck didn't come),
 * because removing it would only make the app record it again (D30).
 */
@Composable
private fun EditEntryDialog(
    item: ActivityItem,
    onSave: (amount: Long, note: String?) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(formatMoney(item.entry.amount.absoluteValue).removePrefix("$")) }
    var note by rememberSaveable { mutableStateOf(item.entry.note ?: "") }
    val recurring = item.kind == ActivityKind.INCOME || item.kind == ActivityKind.FIXED_PAYMENT
    // Recurring ones may be $0; what the user typed must cost or bring something.
    val amount = parseMoney(amountText)?.takeIf { if (recurring) it >= 0 else it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title(item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${formatDate(item.entry.date)} · ${kindLabel(item)}", style = MaterialTheme.typography.bodySmall)
                MoneyField(amountText, { amountText = it }, stringResource(R.string.setup_amount), isError = amount == null)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.activity_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (recurring) {
                    Text(stringResource(R.string.activity_recurring_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(amount!!, note.trim().ifBlank { null }); onDismiss() },
                enabled = amount != null,
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            if (item.removable) {
                TextButton(onClick = { onRemove(); onDismiss() }) { Text(stringResource(R.string.setup_remove)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
            }
        },
    )
}
