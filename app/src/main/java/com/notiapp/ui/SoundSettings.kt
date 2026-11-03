package com.notiapp.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import com.notiapp.R
import com.notiapp.alert.VibrationPattern
import com.notiapp.alert.Vibrations
import com.notiapp.data.AppSettings
import com.notiapp.data.Settings
import kotlinx.coroutines.launch
import android.provider.Settings as SystemSettings

/** Beltoon kiezen en trilpatroon kiezen (met voorbeeld). Wijzigingen worden meteen bewaard. */
@Composable
fun SoundSettings(settings: AppSettings, store: Settings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        // "Standaard beltoon" (of niets) gekozen = de beltoon van de telefoon volgen.
        val custom = picked?.takeIf { it != SystemSettings.System.DEFAULT_RINGTONE_URI }
        scope.launch { store.setRingtone(custom) }
    }

    Column {
        Text(stringResource(R.string.settings_sound), style = MaterialTheme.typography.titleMedium)

        val ringtoneTitle = remember(settings.ringtoneUri) { ringtoneTitle(context, settings.ringtoneUri) }
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    picker.launch(
                        Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.settings_ringtone))
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, SystemSettings.System.DEFAULT_RINGTONE_URI)
                            .putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                settings.ringtoneUri ?: SystemSettings.System.DEFAULT_RINGTONE_URI,
                            )
                    )
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null)
            Column(Modifier.padding(start = 16.dp)) {
                Text(stringResource(R.string.settings_ringtone), style = MaterialTheme.typography.titleSmall)
                Text(ringtoneTitle, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Text(
            stringResource(R.string.settings_vibration),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
        VibrationPattern.entries.forEach { pattern ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = pattern == settings.vibrationPattern,
                        role = Role.RadioButton,
                        onClick = {
                            scope.launch { store.setVibrationPattern(pattern) }
                            Vibrations.preview(context, pattern)
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = pattern == settings.vibrationPattern, onClick = null, modifier = Modifier.padding(12.dp))
                Text(stringResource(pattern.label), modifier = Modifier.weight(1f))
                IconButton(onClick = { Vibrations.preview(context, pattern) }) {
                    Icon(Icons.Filled.PlayArrow, stringResource(R.string.settings_vibration_preview))
                }
            }
        }
        Text(stringResource(R.string.settings_sound_hint), style = MaterialTheme.typography.bodySmall)
    }
}

private fun ringtoneTitle(context: Context, uri: Uri?): String {
    val standard = context.getString(R.string.settings_ringtone_default)
    if (uri == null) return standard
    return runCatching { RingtoneManager.getRingtone(context, uri)?.getTitle(context) }.getOrNull() ?: standard
}
