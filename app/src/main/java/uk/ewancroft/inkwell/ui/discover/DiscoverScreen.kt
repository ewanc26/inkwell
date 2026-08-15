package uk.ewancroft.inkwell.ui.discover

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.ui.components.CreditsView
import uk.ewancroft.inkwell.data.model.common.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel = hiltViewModel(),
    onSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCredits by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember {
        try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${pkg.versionName} (${pkg.longVersionCode})"
        } catch (_: Exception) { "Version 1.0.1 (2)" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Discover") },
                actions = {
                    IconButton(onClick = { showCredits = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                },
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = { viewModel.onQueryChanged(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("Search publications and articles") },
                singleLine = true,
                trailingIcon = {
                    if (uiState.query.isNotBlank()) {
                        IconButton(onClick = { viewModel.onQueryChanged("") }) {
                            Icon(Icons.Outlined.Close, "Clear")
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.search() }
                )
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { viewModel.search() },
                    enabled = uiState.query.isNotBlank() && !uiState.isSearching
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Search")
                }
            }

            when {
                uiState.isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Searching the Standard.site network...", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
                uiState.results.isEmpty() && !uiState.isSearching -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Search, null, Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text("Search the Open Web", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Find Standard.site writing from Leaflet, pckt, Offprint, and independent publishers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
                else -> {
                    val documents = uiState.results.filter { !it.isPublication }
                    val publications = uiState.results.filter { it.isPublication }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (documents.isNotEmpty()) {
                            item {
                                Text("Documents", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                            }
                            items(documents, key = { it.uri }) { result ->
                                SearchResultRow(result = result)
                            }
                        }

                        if (publications.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(12.dp))
                                Text("Publications", style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                            }
                            items(publications, key = { it.uri }) { result ->
                                SearchResultRow(
                                    result = result,
                                    isSubscribed = uiState.subscriptions.containsKey(result.uri),
                                    isSubscriptionPending = result.uri in uiState.pendingSubscriptions,
                                    onToggleSubscription = { viewModel.toggleSubscription(result) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.subscriptionError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissSubscriptionError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.subscriptionError!!)
        }
    }

    if (showCredits) {
        CreditsView(
            appVersion = appVersion,
            onSignOut = onSignOut,
            onDismiss = { showCredits = false },
        )
    }
}

@Composable
private fun SearchResultRow(
    result: SearchResult,
    isSubscribed: Boolean = false,
    isSubscriptionPending: Boolean = false,
    onToggleSubscription: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (result.coverImage != null) {
                AsyncImage(
                    model = result.coverImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(Modifier.weight(1f)) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (result.snippet != null) {
                    Text(
                        result.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        result.platform ?: "standard.site",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (result.handle != null) {
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            result.handle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (result.isPublication && onToggleSubscription != null) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSubscriptionPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        OutlinedIconToggleButton(
                            checked = isSubscribed,
                            onCheckedChange = { onToggleSubscription() },
                        ) {
                            Icon(
                                if (isSubscribed) Icons.Filled.Notifications else Icons.Outlined.NotificationAdd,
                                contentDescription = if (isSubscribed) "Unsubscribe" else "Subscribe",
                                tint = if (isSubscribed) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
