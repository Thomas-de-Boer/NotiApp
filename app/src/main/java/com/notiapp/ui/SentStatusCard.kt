package com.notiapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notiapp.R
import com.notiapp.alert.SentAlerts
import com.notiapp.data.AppSettings
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import kotlinx.coroutines.launch

/** Laat zien wie het laatst verstuurde bericht al gezien heeft, met een knop om het bellen te stoppen. */
@Composable
fun SentStatusCard(settings: AppSettings, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sent by SentAlerts.last.collectAsState()
    var busy by remember { mutableStateOf(false) }
    val alert = sent ?: return

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_last_sent, alert.text), style = MaterialTheme.typography.titleSmall)
            Text(
                if (alert.seenBy.isEmpty()) stringResource(R.string.home_seen_nobody)
                else stringResource(R.string.home_seen_by, alert.seenBy.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            try {
                                FcmSender.send(
                                    context,
                                    FamilyMessage(
                                        type = FamilyMessage.Type.CANCEL,
                                        alertId = alert.alertId,
                                        senderId = settings.senderId,
                                        senderName = settings.name,
                                    ),
                                )
                                SentAlerts.clear()
                            } catch (e: FcmSender.SendException) {
                                onError(e.message.orEmpty())
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) { Text(stringResource(R.string.home_cancel_alert)) }
                TextButton(onClick = SentAlerts::clear) { Text(stringResource(R.string.home_hide)) }
            }
        }
    }
}
