package uk.ewancroft.inkwell.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.text.format.DateUtils
import uk.ewancroft.inkwell.data.remote.InkwellNotification

/**
 * The in-app notification history -- mirrors iOS NotificationsView.swift.
 * Lets the user review documents they were notified about (in case a
 * banner was missed or dismissed) and clear the list. Distinct from the
 * unread *count*, which is cleared just by viewing the Reader tab; the
 * list itself persists until explicitly cleared here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsDialog(
    notifications: List<InkwellNotification>,
    onOpenDocument: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
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
                        title = { Text("Notifications") },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                            }
                        },
                        actions = {
                            if (notifications.isNotEmpty()) {
                                TextButton(onClick = onClearAll) {
                                    Text("Clear All")
                                }
                            }
                        },
                    )
                },
            ) { innerPadding ->
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.padding(bottom = 8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "No notifications yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(innerPadding)) {
                        items(notifications, key = { it.date.toString() + it.documentURI }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onClick = {
                                    onOpenDocument(notification.documentURI)
                                    onDismiss()
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: InkwellNotification, onClick: () -> Unit) {
    val metadata = listOfNotNull(
        notification.publicationName,
        relativeTime(notification.date),
    ).joinToString(" • ")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = listOf(notification.documentTitle, metadata, "Open article")
                    .filter(String::isNotBlank)
                    .joinToString(". ")
            }
            .clickable(role = Role.Button, onClickLabel = "Open article", onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            notification.documentTitle,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            metadata,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Mirrors iOS's use of RelativeDateTimeFormatter. */
private fun relativeTime(epochMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        epochMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
