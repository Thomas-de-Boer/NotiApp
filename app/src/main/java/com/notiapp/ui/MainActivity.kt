package com.notiapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.notiapp.R
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.notiapp.alert.AlertService
import com.notiapp.alert.SentAlerts
import com.notiapp.data.Settings
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleAck(intent)
        val store = Settings(applicationContext)
        subscribeToFamily()
        setContent {
            NotiAppTheme {
                Surface(Modifier.fillMaxSize()) {
                    val settings by store.settings.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()
                    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                    val s = settings ?: return@Surface // laden
                    when {
                        s.name.isBlank() ->
                            SetupScreen(initialName = "") { name -> scope.launch { store.setName(name) } }
                        screen == Screen.PERMISSIONS -> {
                            BackHandler { screen = Screen.SETTINGS }
                            SetupScreen(initialName = s.name) { name ->
                                scope.launch { store.setName(name) }
                                screen = Screen.SETTINGS
                            }
                        }
                        screen == Screen.SETTINGS -> SettingsScreen(
                            settings = s,
                            store = store,
                            onOpenPermissions = { screen = Screen.PERMISSIONS },
                            onBack = { screen = Screen.HOME },
                        )
                        else -> HomeScreen(
                            settings = s,
                            actions = {
                                IconButton(onClick = { screen = Screen.SETTINGS }) {
                                    Icon(Icons.Filled.Settings, stringResource(R.string.settings_open))
                                }
                            },
                            statusCard = { showMessage -> SentStatusCard(s, showMessage) },
                            onSent = SentAlerts::onSent,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAck(intent)
    }

    /** De app is geopend door op een gezinsmelding te tikken: dat telt als "gezien". */
    private fun handleAck(intent: Intent?) {
        val alertId = intent?.getStringExtra(AlertService.EXTRA_ALERT_ID) ?: return
        intent.removeExtra(AlertService.EXTRA_ALERT_ID)
        AlertService.acknowledge(this, alertId)
    }

    /** Alle telefoons luisteren naar hetzelfde gezinskanaal. */
    private fun subscribeToFamily() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.w("NotiApp", "Firebase is niet ingesteld (google-services.json ontbreekt)")
            return
        }
        FirebaseMessaging.getInstance().subscribeToTopic(Settings.TOPIC)
    }
}

private enum class Screen { HOME, SETTINGS, PERMISSIONS }

@Composable
fun NotiAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme(
                primary = Color(0xFFFFB59A),
                onPrimary = Color(0xFF5A1B00),
                primaryContainer = Color(0xFF7A2E0B),
                onPrimaryContainer = Color(0xFFFFDBCE),
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFE8622A),
                primaryContainer = Color(0xFFFFDBCE),
                onPrimaryContainer = Color(0xFF3A0B00),
            )
        },
        content = content,
    )
}
