package com.diegodavid.supersave.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.diegodavid.supersave.AppClock
import com.diegodavid.supersave.R
import java.time.Instant
import java.time.ZoneOffset

// TEMPORARY debug tool (Claude): pretend today is another day, to test week and month changes.

/** Shown at the top of home while pretending, so it's never forgotten. */
@Composable
fun PretendDateBanner() {
    val pretend by AppClock.pretend.collectAsState()
    val date = pretend ?: return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.debug_pretending, formatDate(date)), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { AppClock.setPretend(null) }) { Text(stringResource(R.string.debug_real_date)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PretendDateButton() {
    var picking by remember { mutableStateOf(false) }
    TextButton(onClick = { picking = true }) { Text(stringResource(R.string.debug_pretend)) }
    if (!picking) return

    val state = rememberDatePickerState(
        initialSelectedDateMillis = AppClock.today().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = { picking = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    AppClock.setPretend(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                picking = false
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = { picking = false }) { Text(stringResource(android.R.string.cancel)) } },
    ) { DatePicker(state) }
}
