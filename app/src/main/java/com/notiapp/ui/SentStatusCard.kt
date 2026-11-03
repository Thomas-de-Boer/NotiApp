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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notiapp.R
import com.notiapp.data.AppSettings
import com.notiapp.data.History
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import kotlinx.coroutines.launch

/** Hoe lang het laatst verstuurde bericht bovenaan het hoofdscherm blijft staan. */
private const val SHOW_FOR_MS = 60 * 60 * 1000L

/** Laat zien wie het laatst verstuurde bericht al gezien heeft, met een knop om het bellen te stoppen. */
@Composable
fun SentStatusCard(settings: AppSettings, onError: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries by History.entries.collectAsState()
    var hiddenId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val sent = entries.firstOrNull { it.direction == History.Direction.VERSTUURD } ?: return
    if (sent.alertId == hiddenId || System.currentTimeMillis() - sent.time > SHOW_FOR_MS) return

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_last_sent, sent.text), style = MaterialTheme.typography.titleSmall)
            Text(
                if (sent.seenBy.isEmpty()) stringResource(R.string.home_seen_nobody)
                else stringResource(R.string.home_seen_by, sent.seenBy.joinToString(", ")),
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
                                        alertId = sent.alertId,
                                        senderId = settings.senderId,
                                        senderName = settings.name,
                                    ),
                                )
                                hiddenId = sent.alertId
                            } catch (e: FcmSender.SendException) {
                                onError(e.message.orEmpty())
                            } finally {
                                busy = false
                            }
                        }
                    },
                ) { Text(stringResource(R.string.home_cancel_alert)) }
                TextButton(onClick = { hiddenId = sent.alertId }) { Text(stringResource(R.string.home_hide)) }
            }
        }
    }
}
