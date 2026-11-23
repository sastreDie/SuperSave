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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.ReviewChoice
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.ui.MoneyField
import com.diegodavid.supersave.ui.RadioRow
import com.diegodavid.supersave.ui.RecurringItemForm
import com.diegodavid.supersave.ui.ScreenTitle
import com.diegodavid.supersave.ui.formatMoney
import com.diegodavid.supersave.ui.scheduleSummary
import java.time.DayOfWeek
import java.time.LocalDate

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
    ScreenTitle(stringResource(R.string.setup_welcome_title), stringResource(R.string.setup_welcome_body))
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
    ScreenTitle(stringResource(R.string.setup_balance_title), stringResource(R.string.setup_balance_body))
    MoneyField(
        value = vm.balanceText,
        onValueChange = { vm.balanceText = it },
        label = stringResource(R.string.setup_balance_label),
        isError = vm.balanceText.isNotBlank() && vm.balance == null,
    )
}

@Composable
private fun IncomesStep(vm: SetupViewModel) {
    ScreenTitle(stringResource(R.string.setup_incomes_title), stringResource(R.string.setup_incomes_body))
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
    ScreenTitle(stringResource(R.string.setup_payments_title), stringResource(R.string.setup_payments_body))
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
    ScreenTitle(stringResource(R.string.setup_savings_title), stringResource(R.string.setup_savings_body))
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
    // D28: anything due today is already in the balance the user typed, so count from tomorrow.
    val tomorrow = remember { LocalDate.now().plusDays(1) }

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
            firstDate = tomorrow,
            onSave = { name, amount, schedule -> onAdd(name, amount, schedule); adding = false },
            onCancel = { adding = false },
        )
    } else {
        OutlinedButton(onClick = { adding = true }) { Text(stringResource(addText)) }
    }
}
