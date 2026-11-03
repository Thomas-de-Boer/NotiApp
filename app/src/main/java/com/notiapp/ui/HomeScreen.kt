package com.notiapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notiapp.R
import com.notiapp.data.AppSettings
import com.notiapp.data.Settings
import com.notiapp.fcm.FamilyMessage
import com.notiapp.fcm.FcmSender
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: AppSettings,
    actions: @Composable () -> Unit = {},
    statusCard: @Composable (showMessage: (String) -> Unit) -> Unit = {},
    onSent: (FamilyMessage) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var sending by remember { mutableStateOf(false) }
    var fillInPreset by rememberSaveable { mutableStateOf<String?>(null) }

    val sentText = stringResource(R.string.home_sent)

    fun send(text: String) {
        if (sending) return
        sending = true
        val message = FamilyMessage(
            type = FamilyMessage.Type.ALERT,
            alertId = UUID.randomUUID().toString(),
            senderId = settings.senderId,
            senderName = settings.name,
            text = text,
        )
        scope.launch {
            try {
                FcmSender.send(context, message)
                onSent(message)
                snackbar.showSnackbar(sentText)
            } catch (e: FcmSender.SendException) {
                snackbar.showSnackbar(e.message ?: "")
            } finally {
                sending = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = { actions() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(stringResource(R.string.home_greeting, settings.name), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.home_hint), style = MaterialTheme.typography.bodyMedium)
                }
            }
            item(span = { GridItemSpan(2) }) { statusCard { msg -> scope.launch { snackbar.showSnackbar(msg) } } }
            items(settings.presets) { preset ->
                Card(
                    onClick = {
                        if (preset.contains(Settings.FILL_IN)) fillInPreset = preset else send(preset)
                    },
                    enabled = !sending,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                ) {
                    Text(
                        preset,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 20.dp),
                    )
                }
            }
            item(span = { GridItemSpan(2) }) {
                OutlinedCard(
                    onClick = { fillInPreset = CUSTOM_MESSAGE },
                    enabled = !sending,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
                ) {
                    Text(
                        stringResource(R.string.home_custom),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                    )
                }
            }
        }
    }

    fillInPreset?.let { preset ->
        FillInDialog(
            preset = preset,
            onDismiss = { fillInPreset = null },
            onSend = { text -> fillInPreset = null; send(text) },
        )
    }
}

/** Markeert het "Eigen bericht"-venster (in plaats van een vast bericht met …). */
private const val CUSTOM_MESSAGE = "\u0000eigen"

/**
 * Voor berichten als "We vertrekken naar …": vul het laatste stukje in.
 * Voor [CUSTOM_MESSAGE]: typ zelf het hele bericht.
 */
@Composable
private fun FillInDialog(preset: String, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var extra by rememberSaveable { mutableStateOf("") }
    val custom = preset == CUSTOM_MESSAGE
    val prefix = if (custom) "" else preset.substringBefore(Settings.FILL_IN).trimEnd()
    val suffix = if (custom) "" else preset.substringAfter(Settings.FILL_IN).trimStart()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (custom) stringResource(R.string.home_custom_title) else "$prefix …") },
        text = {
            OutlinedTextField(
                value = extra,
                onValueChange = { extra = it },
                label = { Text(stringResource(if (custom) R.string.home_custom_label else R.string.home_fill_in)) },
                singleLine = !custom,
                maxLines = 4,
            )
        },
        confirmButton = {
            Button(
                onClick = { onSend(listOf(prefix, extra.trim(), suffix).filter { it.isNotEmpty() }.joinToString(" ")) },
                enabled = extra.isNotBlank(),
            ) { Text(stringResource(R.string.home_send)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
