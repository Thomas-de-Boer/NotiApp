package com.notiapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notiapp.R
import com.notiapp.alert.AlertService
import com.notiapp.data.AppSettings
import com.notiapp.data.Settings
import com.notiapp.fcm.FamilyMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(settings: AppSettings, store: Settings, onOpenPermissions: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(settings.name) }
    val presets = remember { mutableStateListOf(*settings.presets.toTypedArray()) }

    fun saveAndClose() {
        scope.launch {
            if (name.isNotBlank()) store.setName(name)
            store.setPresets(presets.toList().ifEmpty { Settings.DEFAULT_PRESETS })
            onBack()
        }
    }
    BackHandler { saveAndClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = ::saveAndClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.setup_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { HorizontalDivider() }
            item { SoundSettings(settings, store) }
            item { HorizontalDivider() }
            item {
                Text(stringResource(R.string.settings_presets), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.settings_presets_hint), style = MaterialTheme.typography.bodySmall)
            }
            itemsIndexed(presets, key = { index, _ -> index }) { index, preset ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = preset, onValueChange = { presets[index] = it }, singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (index > 0) presets.add(index - 1, presets.removeAt(index)) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.settings_up))
                    }
                    IconButton(
                        onClick = { if (index < presets.lastIndex) presets.add(index + 1, presets.removeAt(index)) },
                        enabled = index < presets.lastIndex,
                    ) { Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.settings_down)) }
                    IconButton(onClick = { presets.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, stringResource(R.string.settings_delete))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { presets.add("") }) {
                        Icon(Icons.Filled.Add, null)
                        Text(stringResource(R.string.settings_add))
                    }
                    TextButton(onClick = { presets.clear(); presets.addAll(Settings.DEFAULT_PRESETS) }) {
                        Text(stringResource(R.string.settings_reset))
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Button(
                    onClick = {
                        AlertService.startAlert(
                            context,
                            FamilyMessage(
                                type = FamilyMessage.Type.ALERT,
                                alertId = "test-" + System.currentTimeMillis(),
                                senderId = "test",
                                senderName = context.getString(R.string.app_name),
                                text = context.getString(R.string.settings_test_text),
                            ),
                            ringDelaySeconds = 5,
                            isTest = true,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.settings_test)) }
                Text(stringResource(R.string.settings_test_hint), style = MaterialTheme.typography.bodySmall)
            }
            item {
                OutlinedButton(onClick = onOpenPermissions, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.settings_permissions))
                }
            }
        }
    }
}
