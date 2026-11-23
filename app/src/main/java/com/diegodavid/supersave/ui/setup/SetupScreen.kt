package com.diegodavid.supersave.ui.setup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.ReviewChoice
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import com.diegodavid.supersave.ui.formatDate
import com.diegodavid.supersave.ui.formatMoney
import com.diegodavid.supersave.ui.parseMoney
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * First-run setup (D10 step 4): week start, balance, incomes, fixed payments, savings.
 * One screen at a time with Back / Next. (UI scaffolding by Claude, Nivel 2.)
 */
@Composable
fun SetupScreen(modifier: Modifier = Modifier, vm: SetupViewModel = viewModel()) {
    // The phone's back button goes to the previous step instead of closing the app.
    BackHandler(enabled = vm.step != SetupStep.WELCOME) { vm.back() }

    Column(modifier.fillMaxSize().imePadding().padding(16.dp)) {
        LinearProgressIndicator(
            progress = { vm.stepNumber.toFloat() / vm.stepCount },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.setup_step_of, vm.stepNumber, vm.stepCount),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (vm.step) {
                SetupStep.WELCOME -> WelcomeStep(vm)
                SetupStep.BALANCE -> BalanceStep(vm)
                SetupStep.INCOMES -> IncomesStep(vm)
                SetupStep.PAYMENTS -> PaymentsStep(vm)
                SetupStep.SAVINGS -> SavingsStep(vm)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.step != SetupStep.WELCOME) {
                OutlinedButton(onClick = { vm.back() }) { Text(stringResource(R.string.setup_back)) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { vm.next() }, enabled = vm.canContinue) {
                Text(stringResource(if (vm.isLastStep) R.string.setup_finish else R.string.setup_next))
            }
        }
    }
}

// ---------- Steps ----------

@Composable
private fun WelcomeStep(vm: SetupViewModel) {
    StepTitle(R.string.setup_welcome_title, R.string.setup_welcome_body)
    Text(stringResource(R.string.setup_week_start_question), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        RadioRow(stringResource(R.string.monday), vm.weekStart == DayOfWeek.MONDAY) { vm.weekStart = DayOfWeek.MONDAY }
        RadioRow(stringResource(R.string.sunday), vm.weekStart == DayOfWeek.SUNDAY) { vm.weekStart = DayOfWeek.SUNDAY }
    }
    // TEMPORARY, for testing until the real screens are done.
    TextButton(onClick = { vm.loadTestData() }) { Text(stringResource(R.string.debug_load)) }
}

@Composable
private fun BalanceStep(vm: SetupViewModel) {
    StepTitle(R.string.setup_balance_title, R.string.setup_balance_body)
    MoneyField(
        value = vm.balanceText,
        onValueChange = { vm.balanceText = it },
        label = stringResource(R.string.setup_balance_label),
        isError = vm.balanceText.isNotBlank() && vm.balance == null,
    )
}

@Composable
private fun IncomesStep(vm: SetupViewModel) {
    StepTitle(R.string.setup_incomes_title, R.string.setup_incomes_body)
    RecurringList(
        items = vm.incomes.map { Triple(it.name, it.amount, it.schedule) },
        emptyText = R.string.setup_incomes_empty,
        addText = R.string.setup_add_income,
        onAdd = vm::addIncome,
        onRemove = { vm.incomes.removeAt(it) },
    )
}

@Composable
private fun PaymentsStep(vm: SetupViewModel) {
    StepTitle(R.string.setup_payments_title, R.string.setup_payments_body)
    RecurringList(
        items = vm.payments.map { Triple(it.name, it.amount, it.schedule) },
        emptyText = R.string.setup_payments_empty,
        addText = R.string.setup_add_payment,
        onAdd = vm::addPayment,
        onRemove = { vm.payments.removeAt(it) },
    )
}

@Composable
private fun SavingsStep(vm: SetupViewModel) {
    StepTitle(R.string.setup_savings_title, R.string.setup_savings_body)
    MoneyField(
        value = vm.goalText,
        onValueChange = { vm.goalText = it },
        label = stringResource(R.string.setup_savings_label),
        isError = vm.goal == null,
    )

    Text(stringResource(R.string.setup_review_question), style = MaterialTheme.typography.titleMedium)
    Column(Modifier.selectableGroup()) {
        RadioRow(stringResource(R.string.review_keep), vm.reviewChoice == ReviewChoice.KEEP) { vm.reviewChoice = ReviewChoice.KEEP }
        RadioRow(stringResource(R.string.review_spend), vm.reviewChoice == ReviewChoice.SPEND) { vm.reviewChoice = ReviewChoice.SPEND }
    }
    Text(stringResource(R.string.setup_review_note), style = MaterialTheme.typography.bodySmall)

    // D24: recommend keeping savings apart.
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Text(stringResource(R.string.setup_savings_tip), Modifier.padding(16.dp))
    }
}

// ---------- Incomes and payments: same list, same form ----------

@Composable
private fun RecurringList(
    items: List<Triple<String, Long, Schedule>>,
    emptyText: Int,
    addText: Int,
    onAdd: (String, Long, Schedule) -> Unit,
    onRemove: (Int) -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(false) }

    if (items.isEmpty() && !adding) {
        Text(stringResource(emptyText), style = MaterialTheme.typography.bodyMedium)
    }
    items.forEachIndexed { index, (name, amount, schedule) ->
        ListItem(
            headlineContent = { Text(name) },
            supportingContent = { Text("${formatMoney(amount)} · ${scheduleSummary(schedule)}") },
            trailingContent = {
                TextButton(onClick = { onRemove(index) }) { Text(stringResource(R.string.setup_remove)) }
            },
        )
    }
    if (adding) {
        RecurringItemForm(
            onAdd = { name, amount, schedule -> onAdd(name, amount, schedule); adding = false },
            onCancel = { adding = false },
        )
    } else {
        OutlinedButton(onClick = { adding = true }) { Text(stringResource(addText)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringItemForm(onAdd: (String, Long, Schedule) -> Unit, onCancel: () -> Unit) {
    val today = remember { LocalDate.now() }
    var name by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(ScheduleType.BIWEEKLY) }
    var everyNText by rememberSaveable { mutableStateOf("") }
    var dayText by rememberSaveable { mutableStateOf("") }
    var nextDate by rememberSaveable { mutableStateOf(today) }
    var pickingDate by remember { mutableStateOf(false) }

    val amount = parseMoney(amountText)?.takeIf { it > 0 }
    val everyN = everyNText.toIntOrNull()?.takeIf { it >= 1 }
    val day = dayText.toIntOrNull()?.takeIf { it in 1..31 }
    // anchorDate = today: occurrences before today are already part of the balance
    // the user typed, so they must not be counted again. EVERY_N_DAYS needs a real
    // first date, so the user picks it.
    val schedule: Schedule? = when (type) {
        ScheduleType.EVERY_N_DAYS -> everyN?.let { Schedule(type, nextDate, everyNDays = it) }
        ScheduleType.DAY_OF_MONTH -> day?.let { Schedule(type, today, dayOfMonth = it) }
        else -> Schedule(type, today)
    }
    val valid = name.isNotBlank() && amount != null && schedule != null

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.setup_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            MoneyField(
                value = amountText,
                onValueChange = { amountText = it },
                label = stringResource(R.string.setup_amount),
                isError = amountText.isNotBlank() && amount == null,
            )

            Text(stringResource(R.string.setup_how_often), style = MaterialTheme.typography.titleSmall)
            Column(Modifier.selectableGroup()) {
                ScheduleType.entries.forEach { t ->
                    RadioRow(scheduleTypeLabel(t), type == t) { type = t }
                }
            }

            when (type) {
                ScheduleType.EVERY_N_DAYS -> {
                    OutlinedTextField(
                        value = everyNText,
                        onValueChange = { everyNText = it.filter(Char::isDigit) },
                        label = { Text(stringResource(R.string.setup_every_n_label)) },
                        isError = everyNText.isNotBlank() && everyN == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = { pickingDate = true }) {
                        Text(stringResource(R.string.setup_next_date, formatDate(nextDate)))
                    }
                }
                ScheduleType.DAY_OF_MONTH -> OutlinedTextField(
                    value = dayText,
                    onValueChange = { dayText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.setup_day_of_month_label)) },
                    isError = dayText.isNotBlank() && day == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                else -> {}
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onCancel) { Text(stringResource(android.R.string.cancel)) }
                Button(onClick = { onAdd(name.trim(), amount!!, schedule!!) }, enabled = valid) {
                    Text(stringResource(R.string.setup_add))
                }
            }
        }
    }

    if (pickingDate) {
        // The date picker works in UTC milliseconds; convert to and from LocalDate.
        val todayMillis = today.toUtcMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = nextDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        nextDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        ) { DatePicker(state) }
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@Composable
private fun scheduleTypeLabel(type: ScheduleType): String = stringResource(
    when (type) {
        ScheduleType.END_OF_MONTH -> R.string.schedule_end_of_month
        ScheduleType.START_OF_MONTH -> R.string.schedule_start_of_month
        ScheduleType.BIWEEKLY -> R.string.schedule_biweekly
        ScheduleType.EVERY_N_DAYS -> R.string.schedule_every_n_days
        ScheduleType.DAY_OF_MONTH -> R.string.schedule_day_of_month
    }
)

/** "Quincena (15th and end of month)", "Every 14 days, next on Nov 28, 2026", "On the 10th". */
@Composable
private fun scheduleSummary(schedule: Schedule): String = when (schedule.type) {
    ScheduleType.EVERY_N_DAYS -> {
        val n = schedule.everyNDays ?: 0
        pluralStringResource(R.plurals.schedule_every_n_summary, n, n, formatDate(schedule.anchorDate))
    }
    ScheduleType.DAY_OF_MONTH -> stringResource(R.string.schedule_day_of_month_summary, schedule.dayOfMonth ?: 0)
    else -> scheduleTypeLabel(schedule.type)
}

// ---------- Small shared pieces ----------

@Composable
private fun StepTitle(title: Int, body: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun MoneyField(value: String, onValueChange: (String) -> Unit, label: String, isError: Boolean) {
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
private fun RadioRow(label: String, selected: Boolean, onSelect: () -> Unit) {
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
