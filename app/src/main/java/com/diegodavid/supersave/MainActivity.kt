package com.diegodavid.supersave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.ui.CorrectBalanceScreen
import com.diegodavid.supersave.ui.HomeScreen
import com.diegodavid.supersave.ui.MainViewModel
import com.diegodavid.supersave.ui.SettingsScreen
import com.diegodavid.supersave.ui.WeighScreen
import com.diegodavid.supersave.ui.setup.SetupScreen
import com.diegodavid.supersave.ui.theme.SuperSaveTheme
import kotlinx.coroutines.flow.map

/** The screens after setup. Home is the start; the others go back to it. */
private enum class Screen { HOME, WEIGH, CORRECT, SETTINGS }

/**
 * The one screen Android opens when you tap the app icon.
 * Compose draws everything inside setContent { ... }.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = SuperSaveDatabase.get(this)
        setContent {
            SuperSaveTheme {
                // null while the database is still loading; then true/false.
                // It updates by itself: finishing setup shows the home screen,
                // and "Clear all data" brings setup back.
                val isSetUp by remember { db.settings().observe().map { it != null } }
                    .collectAsStateWithLifecycle(initialValue = null)

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // consumeWindowInsets: the Scaffold already made room for the system bars,
                    // so imePadding() on a screen only adds the part of the keyboard above them.
                    val modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                    when (isSetUp) {
                        null -> {}
                        false -> SetupScreen(modifier)
                        true -> MainScreens(modifier)
                    }
                }
            }
        }
    }
}

/**
 * Simple navigation: one variable says which screen is showing. rememberSaveable keeps
 * it when the phone rotates; the back button returns to home.
 */
@Composable
private fun MainScreens(modifier: Modifier, vm: MainViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val home = { screen = Screen.HOME }
    BackHandler(enabled = screen != Screen.HOME, onBack = home)

    when (screen) {
        Screen.HOME -> HomeScreen(
            vm,
            onWeigh = { screen = Screen.WEIGH },
            onCorrect = { screen = Screen.CORRECT },
            onSettings = { screen = Screen.SETTINGS },
            modifier = modifier,
        )
        Screen.WEIGH -> WeighScreen(vm, onDone = home, modifier = modifier)
        Screen.CORRECT -> CorrectBalanceScreen(vm, onDone = home, modifier = modifier)
        Screen.SETTINGS -> SettingsScreen(vm, onBack = home, modifier = modifier)
    }
}
