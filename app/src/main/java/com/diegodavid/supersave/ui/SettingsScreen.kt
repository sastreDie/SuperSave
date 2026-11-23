package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.ReviewChoice
import com.diegodavid.supersave.data.Schedule
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Everything from setup, editable: week start (D2), savings goal, review default (D21),
 * recurring incomes (D7) and fixed payments (D15). The balance is changed with
 * "Correct my balance" instead (D14). (UI by Claude, Nivel 2.)
 */
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val incomes by vm.incomes.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    val s = settings ?: return

    // Starts with the saved goal, "2,000.00".
    var goalText by rememberSaveable { mutableStateOf(formatMoney(s.monthlySavingsGoal).removePrefix("$")) }
    val goal = if (goalText.isBlank()) 0L else parseMoney(goalText)?.takeIf { it >= 0 }

    // Which form is open: "income:new", "income:12", "payment:new", "payment:3", or null.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var stopping by remember { mutableStateOf<Stop?>(null) }
    // Changes made here count from today (unlike setup, which counts from tomorrow, D28).
    val today = remember { LocalDate.now() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text(stringResource(R.string.setup_back)) }
        }
        // D3: frozen weeks don't change.
        Text(stringResource(R.string.settings_next_week_note), style = MaterialTheme.typography.bodySmall)

        SectionTitle(stringResource(R.string.setup_week_start_question))
        Column(Modifier.selectableGroup()) {
            for ((day, label) in listOf(DayOfWeek.MONDAY to R.string.monday, DayOfWeek.SUNDAY to R.string.sunday)) {
                RadioRow(stringResource(label), s.weekStartDayOfWeek == day) {
                    vm.saveSettings(s.copy(weekStartDayOfWeek = day))
                }
            }
        }

        SectionTitle(stringResource(R.string.setup_savings_label))
        MoneyField(goalText, { goalText = it }, stringResource(R.string.setup_savings_label), isError = goal == null)
        Button(
            // clearFocus closes the keyboard; otherwise it keeps popping back up.
            onClick = { vm.saveSettings(s.copy(monthlySavingsGoal = goal!!)); focusManager.clearFocus() },
            enabled = goal != null && goal != s.monthlySavingsGoal,
        ) { Text(stringResource(R.string.settings_save_goal)) }

        SectionTitle(stringResource(R.string.setup_review_question))
        Column(Modifier.selectableGroup()) {
            for ((choice, label) in listOf(ReviewChoice.KEEP to R.string.review_keep, ReviewChoice.SPEND to R.string.review_spend)) {
                RadioRow(stringResource(label), s.defaultReviewChoice == choice) {
                    vm.saveSettings(s.copy(defaultReviewChoice = choice))
                }
            }
        }

        // ----- Incomes -----
        SectionTitle(stringResource(R.string.setup_incomes_title))
        if (incomes.isEmpty() && editing != "income:new") Text(stringResource(R.string.setup_incomes_empty))
        incomes.forEach { income ->
            if (editing == "income:${income.id}") {
                RecurringItemForm(
                    firstDate = today,
                    initialName = income.name, initialAmount = income.amount, initialSchedule = income.schedule,
                    saveText = stringResource(R.string.settings_save),
                    onSave = { name, amount, schedule ->
                        vm.updateIncome(income.copy(name = name, amount = amount, schedule = schedule)); editing = null
                    },
                    onCancel = { editing = null },
                )
            } else {
                ItemRow(income.name, income.amount, income.schedule,
                    onEdit = { editing = "income:${income.id}" },
                    onStop = { stopping = Stop(income.id, income.name, isIncome = true) })
            }
        }
        if (editing == "income:new") {
            RecurringItemForm(
                firstDate = today,
                onSave = { name, amount, schedule ->
                    vm.addIncome(com.diegodavid.supersave.data.RecurringIncome(name = name, amount = amount, schedule = schedule))
                    editing = null
                },
                onCancel = { editing = null },
            )
        } else {
            OutlinedButton(onClick = { editing = "income:new" }) { Text(stringResource(R.string.setup_add_income)) }
        }

        // ----- Fixed payments -----
        SectionTitle(stringResource(R.string.setup_payments_title))
        if (payments.isEmpty() && editing != "payment:new") Text(stringResource(R.string.setup_payments_empty))
        payments.forEach { payment ->
            if (editing == "payment:${payment.id}") {
                RecurringItemForm(
                    firstDate = today,
                    initialName = payment.name, initialAmount = payment.amount, initialSchedule = payment.schedule,
                    saveText = stringResource(R.string.settings_save),
                    onSave = { name, amount, schedule ->
                        vm.updatePayment(payment.copy(name = name, amount = amount, schedule = schedule)); editing = null
                    },
                    onCancel = { editing = null },
                )
            } else {
                ItemRow(payment.name, payment.amount, payment.schedule,
                    onEdit = { editing = "payment:${payment.id}" },
                    onStop = { stopping = Stop(payment.id, payment.name, isIncome = false) })
            }
        }
        if (editing == "payment:new") {
            RecurringItemForm(
                firstDate = today,
                onSave = { name, amount, schedule ->
                    vm.addPayment(com.diegodavid.supersave.data.FixedPayment(name = name, amount = amount, schedule = schedule))
                    editing = null
                },
                onCancel = { editing = null },
            )
        } else {
            OutlinedButton(onClick = { editing = "payment:new" }) { Text(stringResource(R.string.setup_add_payment)) }
        }
    }

    stopping?.let { stop ->
        ConfirmDialog(
            title = stringResource(R.string.settings_stop_title, stop.name),
            body = stringResource(if (stop.isIncome) R.string.settings_stop_income_body else R.string.settings_stop_payment_body),
            confirm = stringResource(R.string.settings_stop),
            onConfirm = { if (stop.isIncome) vm.endIncome(stop.id) else vm.cancelPayment(stop.id) },
            onDismiss = { stopping = null },
        )
    }
}

/** An income or payment the user wants to stop (D15). */
private data class Stop(val id: Long, val name: String, val isIncome: Boolean)

@Composable
private fun ItemRow(name: String, amount: Long, schedule: Schedule, onEdit: () -> Unit, onStop: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text("${formatMoney(amount)} · ${scheduleSummary(schedule)}") },
        trailingContent = {
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.settings_edit)) }
                TextButton(onClick = onStop) { Text(stringResource(R.string.settings_stop)) }
            }
        },
    )
}
