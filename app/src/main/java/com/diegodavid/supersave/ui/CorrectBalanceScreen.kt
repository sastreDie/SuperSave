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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import kotlin.math.absoluteValue

/**
 * "Correct my balance" (D14, D18): type the real bank balance; the difference counts
 * right away, this week included. (UI by Claude, Nivel 2.)
 */
@Composable
fun CorrectBalanceScreen(vm: MainViewModel, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val home by vm.home.collectAsStateWithLifecycle()
    val tracked = home?.balance ?: return
    var text by rememberSaveable { mutableStateOf("") }
    val real = parseMoney(text)

    Column(modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScreenTitle(stringResource(R.string.correct_title), stringResource(R.string.correct_body))
            Text(stringResource(R.string.correct_tracked, formatMoney(tracked)), style = MaterialTheme.typography.titleMedium)
            MoneyField(
                value = text,
                onValueChange = { text = it },
                label = stringResource(R.string.correct_label),
                isError = text.isNotBlank() && real == null,
            )
            if (real != null) {
                val diff = real - tracked
                Text(
                    when {
                        diff > 0 -> stringResource(R.string.correct_more, formatMoney(diff))
                        diff < 0 -> stringResource(R.string.correct_less, formatMoney(diff.absoluteValue))
                        else -> stringResource(R.string.correct_same)
                    }
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = onDone) { Text(stringResource(android.R.string.cancel)) }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { vm.correctBalance(real!!); onDone() },
                enabled = real != null && real != tracked,
            ) { Text(stringResource(R.string.correct_save)) }
        }
    }
}
