package com.notiapp.ui

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.notiapp.data.AppSettings
import com.notiapp.data.Member
import com.notiapp.data.Members
import kotlinx.coroutines.launch

/** "Wie heeft de app?": jezelf en alle andere gezinsleden die de app hebben. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(settings: AppSettings, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { Members(context.applicationContext) }
    val members by store.members.collectAsState(initial = emptyList())
    var refreshing by remember { mutableStateOf(false) }
    var toRemove by remember { mutableStateOf<Member?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.members_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
                actions = {
                    if (refreshing) {
                        CircularProgressIndicator(Modifier.size(24.dp).padding(end = 4.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            refreshing = true
                            scope.launch {
                                store.announce(force = true)
                                store.checkMembers(force = true)
                                refreshing = false
                            }
                        }) { Icon(Icons.Filled.Refresh, stringResource(R.string.members_refresh)) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { MemberRow(name = settings.name, detail = stringResource(R.string.members_you), onRemove = null) }
            items(members.sortedBy { it.name.lowercase() }, key = { it.id }) { member ->
                val justNow = stringResource(R.string.members_just_now)
                val seen = if (System.currentTimeMillis() - member.lastSeen < DateUtils.MINUTE_IN_MILLIS) justNow
                else DateUtils.getRelativeTimeSpanString(
                    member.lastSeen, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                ).toString()
                MemberRow(
                    name = member.name,
                    detail = stringResource(R.string.members_last_seen, seen),
                    onRemove = { toRemove = member },
                )
            }
            item {
                Text(
                    stringResource(if (members.isEmpty()) R.string.members_empty else R.string.members_hint),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    toRemove?.let { member ->
        AlertDialog(
            onDismissRequest = { toRemove = null },
            title = { Text(stringResource(R.string.members_remove_title, member.name)) },
            text = { Text(stringResource(R.string.members_remove_text)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { store.remove(member.id) }
                    toRemove = null
                }) { Text(stringResource(R.string.settings_delete)) }
            },
            dismissButton = { TextButton(onClick = { toRemove = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun MemberRow(name: String, detail: String, onRemove: (() -> Unit)?) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Person, contentDescription = null)
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, stringResource(R.string.settings_delete)) }
            }
        }
    }
}
