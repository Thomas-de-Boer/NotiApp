package com.notiapp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            GezinsbelTheme {
                Surface(Modifier.fillMaxSize()) {
                    val settings by store.settings.collectAsState(initial = null)
                    val scope = rememberCoroutineScope()
                    when (val s = settings) {
                        null -> Unit // laden
                        else -> if (s.name.isBlank()) {
                            SetupScreen(initialName = "") { name -> scope.launch { store.setName(name) } }
                        } else {
                            HomeScreen(
                                settings = s,
                                statusCard = { SentStatusCard() },
                                onSent = SentAlerts::onSent,
                            )
                        }
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
            Log.w("Gezinsbel", "Firebase is niet ingesteld (google-services.json ontbreekt)")
            return
        }
        FirebaseMessaging.getInstance().subscribeToTopic(Settings.TOPIC)
    }
}

@Composable
fun GezinsbelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFFFFB59A))
        else lightColorScheme(primary = Color(0xFFE8622A)),
        content = content,
    )
}
