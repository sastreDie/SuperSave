package com.diegodavid.supersave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.diegodavid.supersave.data.SuperSaveDatabase
import com.diegodavid.supersave.ui.DebugScreen
import com.diegodavid.supersave.ui.setup.SetupScreen
import com.diegodavid.supersave.ui.theme.SuperSaveTheme
import kotlinx.coroutines.flow.map

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
                        // The real home screen replaces DebugScreen next.
                        true -> DebugScreen(modifier)
                    }
                }
            }
        }
    }
}
