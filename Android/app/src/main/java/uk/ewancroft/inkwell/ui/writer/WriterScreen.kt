package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.ewancroft.inkwell.ui.components.CreditsView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriterScreen(
    viewModel: WriterViewModel = hiltViewModel(),
    onSignOut: () -> Unit = {},
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    var pubExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }

    val formats = listOf("Leaflet", "Markpub", "pckt", "Offprint")

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember {
        try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${pkg.versionName} (${pkg.longVersionCode})"
        } catch (_: Exception) { "Version 1.2.0 (4)" }
    }

    LaunchedEffect(Unit) {
        viewModel.loadPublications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write") },
                navigationIcon = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCredits = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.isLoadingPublications) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.publications.isEmpty()) {
                Text("No publications found.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create a Publication")
                }
            } else {
                // Publication picker
                Box {
                    OutlinedButton(
                        onClick = { pubExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            uiState.selectedPublication?.name ?: "Select a publication...",
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = pubExpanded,
                        onDismissRequest = { pubExpanded = false },
                    ) {
                        uiState.publications.forEach { pub ->
                            DropdownMenuItem(
                                text = { Text(pub.name) },
                                onClick = {
                                    viewModel.selectPublication(pub)
                                    pubExpanded = false
                                },
                            )
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
                TextButton(onClick = { viewModel.showCreateDialog() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Publication")
                }
            }

            // Verification status
            if (uiState.isVerifyingPublication) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(
                        "Verifying publication...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (uiState.verificationMessage != null) {
                Text(
                    uiState.verificationMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.verifiedPublicationUri != null)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Format picker
            Box {
                OutlinedButton(
                    onClick = { formatExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(uiState.selectedFormat, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(
                    expanded = formatExpanded,
                    onDismissRequest = { formatExpanded = false },
                ) {
                    formats.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(format) },
                            onClick = {
                                viewModel.selectFormat(format)
                                formatExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.title, onValueChange = { viewModel.onTitleChanged(it) },
                label = { Text("Title") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.description, onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text("Description (optional)") }, maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.path, onValueChange = { viewModel.onPathChanged(it) },
                label = { Text("Path (optional, e.g. my-post)") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.markdown, onValueChange = { viewModel.onMarkdownChanged(it) },
                label = { Text("Content (Markdown)") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 10
            )

            if (uiState.publishError != null) {
                Text(
                    uiState.publishError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (uiState.publishSuccess != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        uiState.publishSuccess!!,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.publishedUri != null) {
                        val publishedUri = requireNotNull(uiState.publishedUri)
                        TextButton(
                            onClick = {
                                onNavigateToPost(publishedUri, null, null, null, null)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Text("View post")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, Modifier.size(16.dp))
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.publish() },
                enabled = uiState.title.isNotBlank() && uiState.selectedPublication != null && !uiState.isPublishing && !uiState.isVerifyingPublication,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Publish")
            }
        }

            if (uiState.showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissCreateDialog() },
                    title = { Text("New Publication") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = uiState.createUrl,
                                onValueChange = { viewModel.onCreateUrlChanged(it) },
                                label = { Text("URL (e.g. https://mysite.com)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.createName,
                                onValueChange = { viewModel.onCreateNameChanged(it) },
                                label = { Text("Publication Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.createDescription,
                                onValueChange = { viewModel.onCreateDescriptionChanged(it) },
                                label = { Text("Description (optional)") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.createError != null) {
                                Text(
                                    uiState.createError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.createPublication() },
                            enabled = uiState.createUrl.isNotBlank() && uiState.createName.isNotBlank() && !uiState.isCreating
                        ) {
                            if (uiState.isCreating) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissCreateDialog() }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
