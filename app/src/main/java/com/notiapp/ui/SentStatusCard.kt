package com.notiapp.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notiapp.R
import com.notiapp.alert.SentAlerts

/** Laat zien wie het laatst verstuurde bericht al gezien heeft. */
@Composable
fun SentStatusCard(actions: @Composable () -> Unit = {}) {
    val sent by SentAlerts.last.collectAsState()
    val alert = sent ?: return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_last_sent, alert.text), style = MaterialTheme.typography.titleSmall)
            Text(
                if (alert.seenBy.isEmpty()) stringResource(R.string.home_seen_nobody)
                else stringResource(R.string.home_seen_by, alert.seenBy.joinToString(", ")),
                style = MaterialTheme.typography.bodyMedium,
            )
            actions()
        }
    }
}
