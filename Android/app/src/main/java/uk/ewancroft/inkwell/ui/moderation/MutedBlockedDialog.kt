package uk.ewancroft.inkwell.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import uk.ewancroft.inkwell.data.repository.BlockedActorEntry
import uk.ewancroft.inkwell.data.repository.ModeratedActor

/**
 * "Muted & Blocked Accounts" screen — two lists, fetched from
 * `app.bsky.graph.getMutes` / the signed-in user's own
 * `app.bsky.graph.block` records, each with an Unmute/Unblock action.
 * Opened from Settings → Account. Mirrors iOS's MutedBlockedView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MutedBlockedDialog(
    onDismiss: () -> Unit,
    viewModel: MutedBlockedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Muted & Blocked") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Muted (${state.mutes.size})") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Blocked (${state.blocks.size})") },
                        )
                    }

                    state.error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(16.dp),
                        )
                    }

                    when {
                        state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.semantics {
                                    contentDescription = "Loading moderation settings"
                                },
                            )
                        }
                        selectedTab == 0 -> MutedList(
                            mutes = state.mutes,
                            removingKeys = state.removingKeys,
                            onUnmute = viewModel::unmute,
                        )
                        else -> BlockedList(
                            blocks = state.blocks,
                            removingKeys = state.removingKeys,
                            onUnblock = viewModel::unblock,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MutedList(
    mutes: List<ModeratedActor>,
    removingKeys: Set<String>,
    onUnmute: (String) -> Unit,
) {
    if (mutes.isEmpty()) {
        EmptyState("No muted accounts.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(mutes, key = { it.did }) { actor ->
            ActorRow(
                actor = actor,
                actionLabel = "Unmute",
                isRemoving = actor.did in removingKeys,
                onAction = { onUnmute(actor.did) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun BlockedList(
    blocks: List<BlockedActorEntry>,
    removingKeys: Set<String>,
    onUnblock: (BlockedActorEntry) -> Unit,
) {
    if (blocks.isEmpty()) {
        EmptyState("No blocked accounts.")
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(blocks, key = { it.rkey }) { entry ->
            ActorRow(
                actor = entry.actor,
                actionLabel = "Unblock",
                isRemoving = entry.rkey in removingKeys,
                onAction = { onUnblock(entry) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActorRow(
    actor: ModeratedActor,
    actionLabel: String,
    isRemoving: Boolean,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(actor.displayName ?: actor.handle, style = MaterialTheme.typography.bodyLarge)
            Text(
                "@${actor.handle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isRemoving) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(20.dp)
                    .semantics {
                        contentDescription = "$actionLabel @${actor.handle} in progress"
                    },
                strokeWidth = 2.dp,
            )
        } else {
            TextButton(
                onClick = onAction,
                modifier = Modifier.semantics {
                    contentDescription = "$actionLabel @${actor.handle}"
                },
            ) {
                Text(actionLabel)
            }
        }
    }
}
