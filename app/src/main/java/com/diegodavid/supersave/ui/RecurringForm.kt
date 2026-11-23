package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.diegodavid.supersave.R
import com.diegodavid.supersave.data.Schedule
import com.diegodavid.supersave.data.ScheduleType
import com.diegodavid.supersave.engine.DateRange
import com.diegodavid.supersave.engine.occurrencesIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The form for one recurring income or fixed payment: name, amount, schedule (D8).
 * Used by setup (to add) and by settings (to add or edit). (UI scaffolding by Claude.)
 *
 * [firstDate]: the first day it can count from; it becomes the schedule's anchorDate,
 * so nothing before it is ever recorded twice. Setup passes tomorrow (D28), settings today.
 * [initial*]: the values to start with when editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringItemForm(
    firstDate: LocalDate,
    onSave: (name: String, amount: Long, schedule: Schedule) -> Unit,
    onCancel: () -> Unit,
    initialName: String = "",
    initialAmount: Long? = null,
    initialSchedule: Schedule? = null,
    saveText: String = stringResource(R.string.setup_add),
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var amountText by rememberSaveable { mutableStateOf(initialAmount?.let { formatMoney(it).removePrefix("$") } ?: "") }
    var type by rememberSaveable { mutableStateOf(initialSchedule?.type ?: ScheduleType.BIWEEKLY) }
    var everyNText by rememberSaveable { mutableStateOf(initialSchedule?.everyNDays?.toString() ?: "") }
    var dayText by rememberSaveable { mutableStateOf(initialSchedule?.dayOfMonth?.toString() ?: "") }
    var nextDate by rememberSaveable { mutableStateOf(nextOccurrence(initialSchedule, firstDate)) }
    var pickingDate by remember { mutableStateOf(false) }

    val amount = parseMoney(amountText)?.takeIf { it > 0 }
    val everyN = everyNText.toIntOrNull()?.takeIf { it >= 1 }
    val day = dayText.toIntOrNull()?.takeIf { it in 1..31 }
    val schedule: Schedule? = when (type) {
        // EVERY_N_DAYS needs a real first date, so the user picks it.
        ScheduleType.EVERY_N_DAYS -> everyN?.let { Schedule(type, nextDate, everyNDays = it) }
        ScheduleType.DAY_OF_MONTH -> day?.let { Schedule(type, firstDate, dayOfMonth = it) }
        else -> Schedule(type, firstDate)
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
                ScheduleType.entries.forEach { t -> RadioRow(scheduleTypeLabel(t), type == t) { type = t } }
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
                Button(onClick = { onSave(name.trim(), amount!!, schedule!!) }, enabled = valid) {
                    Text(saveText)
                }
            }
        }
    }

    if (pickingDate) {
        // The date picker works in UTC milliseconds; convert to and from LocalDate.
        val firstMillis = firstDate.toUtcMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = nextDate.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= firstMillis
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

/** For an existing "every N days" schedule, its next date on or after [from], keeping the same rhythm. */
private fun nextOccurrence(schedule: Schedule?, from: LocalDate): LocalDate {
    val n = schedule?.everyNDays ?: return from
    if (schedule.type != ScheduleType.EVERY_N_DAYS) return from
    val start = maxOf(from, schedule.anchorDate)
    return occurrencesIn(schedule, DateRange(start, start.plusDays(n.toLong() - 1))).firstOrNull() ?: start
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

@Composable
fun scheduleTypeLabel(type: ScheduleType): String = stringResource(
    when (type) {
        ScheduleType.END_OF_MONTH -> R.string.schedule_end_of_month
        ScheduleType.START_OF_MONTH -> R.string.schedule_start_of_month
        ScheduleType.BIWEEKLY -> R.string.schedule_biweekly
        ScheduleType.EVERY_N_DAYS -> R.string.schedule_every_n_days
        ScheduleType.DAY_OF_MONTH -> R.string.schedule_day_of_month
    }
)

/** "Quincena (15th and end of month)", "Every 14 days, next on Nov 28, 2026", "Day 10 of every month". */
@Composable
fun scheduleSummary(schedule: Schedule): String = when (schedule.type) {
    ScheduleType.EVERY_N_DAYS -> {
        val n = schedule.everyNDays ?: 0
        pluralStringResource(R.plurals.schedule_every_n_summary, n, n, formatDate(nextOccurrence(schedule, LocalDate.now())))
    }
    ScheduleType.DAY_OF_MONTH -> stringResource(R.string.schedule_day_of_month_summary, schedule.dayOfMonth ?: 0)
    else -> scheduleTypeLabel(schedule.type)
}
