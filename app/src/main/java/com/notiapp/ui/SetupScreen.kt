package com.notiapp.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.notiapp.R

/** Eerste start: naam invullen en toestemmingen geven. Ook bruikbaar vanuit de instellingen. */
@Composable
fun SetupScreen(initialName: String, onDone: (String) -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initialName) }

    // Bij terugkomen uit de systeeminstellingen de status opnieuw bekijken.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) { refresh++; onPauseOrDispose { } }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    val notifOk = remember(refresh) { Permissions.hasNotifications(context) }
    val fullScreenOk = remember(refresh) { Permissions.hasFullScreen(context) }
    val batteryOk = remember(refresh) { Permissions.ignoresBatteryOptimizations(context) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.setup_intro))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.setup_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(stringResource(R.string.setup_permissions_title), style = MaterialTheme.typography.titleMedium)

        PermissionRow(
            ok = notifOk,
            title = stringResource(R.string.perm_notifications),
            explanation = stringResource(R.string.perm_notifications_why),
        ) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Permissions.appSettingsIntent(context))
        }
        PermissionRow(
            ok = fullScreenOk,
            title = stringResource(R.string.perm_fullscreen),
            explanation = stringResource(R.string.perm_fullscreen_why),
        ) { context.startActivity(Permissions.fullScreenSettingsIntent(context)) }
        PermissionRow(
            ok = batteryOk,
            title = stringResource(R.string.perm_battery),
            explanation = stringResource(R.string.perm_battery_why),
        ) { context.startActivity(Permissions.batterySettingsIntent(context)) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onDone(name.trim()) },
            enabled = name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.setup_done)) }
        if (!(notifOk && fullScreenOk && batteryOk)) {
            Text(stringResource(R.string.setup_permissions_warning), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PermissionRow(ok: Boolean, title: String, explanation: String, onGrant: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (ok) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(explanation, style = MaterialTheme.typography.bodySmall)
            }
            if (!ok) OutlinedButton(onClick = onGrant) { Text(stringResource(R.string.perm_grant)) }
        }
    }
}
