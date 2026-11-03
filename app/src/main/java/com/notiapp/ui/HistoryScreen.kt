package com.notiapp.ui

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notiapp.R
import com.notiapp.data.History

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val entries by History.entries.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val direction = if (tab == 0) History.Direction.ONTVANGEN else History.Direction.VERSTUURD
    val shown = entries.filter { it.direction == direction }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.history_received)) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.history_sent)) })
            }
            if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.direction.name + it.alertId }) { HistoryRow(it) }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: History.Entry) {
    val context = LocalContext.current
    val time = DateUtils.getRelativeDateTimeString(
        context, entry.time, DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME,
    ).toString()
    val everyone = stringResource(R.string.to_everyone)
    val who = if (entry.direction == History.Direction.ONTVANGEN) {
        stringResource(R.string.history_from, entry.from)
    } else {
        stringResource(R.string.history_to, entry.to.ifEmpty { listOf(everyone) }.joinToString(", "))
    }
    val (status, color) = when {
        entry.direction == History.Direction.VERSTUURD && entry.seenBy.isEmpty() ->
            stringResource(R.string.home_seen_nobody) to MaterialTheme.colorScheme.onSurfaceVariant
        entry.direction == History.Direction.VERSTUURD ->
            stringResource(R.string.home_seen_by, entry.seenBy.joinToString(", ")) to Color(0xFF2E7D32)
        entry.status == History.Status.GEZIEN -> stringResource(R.string.status_seen) to Color(0xFF2E7D32)
        entry.status == History.Status.GEMIST -> stringResource(R.string.status_missed) to MaterialTheme.colorScheme.error
        entry.status == History.Status.GEANNULEERD ->
            stringResource(R.string.status_cancelled) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> stringResource(R.string.status_new) to MaterialTheme.colorScheme.primary
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(entry.text, style = MaterialTheme.typography.titleMedium)
            val icon = when (entry.mode) { "BELLEN" -> "📞"; "STIL" -> "🔕"; else -> "🔔" }
            Text("$icon $who · $time", style = MaterialTheme.typography.bodySmall)
            Text(status, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
