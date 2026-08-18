package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.ewancroft.inkwell.ui.components.CreditsView
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.ui.reader.MarkdownRendererView

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
    var showDocumentPicker by remember { mutableStateOf(false) }

    val formats = listOf("Leaflet", "Markpub", "pckt", "Offprint")

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember { uk.ewancroft.inkwell.util.appVersionString(context) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
        if (bytes != null) {
            viewModel.uploadImage(bytes, mimeType)
        }
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

            if (uiState.editingDocumentUri != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "Editing existing document",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.cancelEditing() }) {
                            Text("Cancel", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { showDocumentPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.editingDocumentUri != null) "Change document" else "Edit existing document")
                }
            }

            if (showDocumentPicker) {
                DocumentPickerDialog(
                    publications = uiState.publications,
                    selectedPublication = uiState.selectedPublication,
                    onSelectDocument = { uri ->
                        viewModel.loadDocumentForEditing(uri)
                        showDocumentPicker = false
                    },
                    onDismiss = { showDocumentPicker = false },
                )
            }

            // Format picker
            // Format picker (disabled when editing existing document)
            val isFormatLocked = uiState.editingDocumentUri != null
            Box {
                OutlinedButton(
                    onClick = { if (!isFormatLocked) formatExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isFormatLocked,
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

            // Loss reporting
            if (uiState.lostFeatures.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            "This post contains ${uiState.lostFeatures.joinToString(", ")} that markdown can't represent. Saving will drop those.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Formatting toolbar
            FormattingToolbar(
                canUploadImages = when (uiState.selectedFormat) {
                    "Markpub" -> false
                    else -> true
                },
                onImagePicker = { imagePickerLauncher.launch("image/*") },
            )

            // Preview toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Switch(
                    checked = uiState.showPreview,
                    onCheckedChange = { viewModel.togglePreview() },
                )
            }

            // Content editor
            OutlinedTextField(
                value = uiState.markdown, onValueChange = { viewModel.onMarkdownChanged(it) },
                label = { Text("Content (Markdown)") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 10
            )

            // Live preview
            if (uiState.showPreview && uiState.markdown.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 400.dp),
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.padding(8.dp),
                    ) {
                        item {
                            MarkdownRendererView(markdown = uiState.markdown)
                        }
                    }
                }
            }

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
                enabled = uiState.title.isNotBlank() && uiState.selectedPublication != null && uiState.verifiedPublicationUri != null && !uiState.isPublishing && !uiState.isVerifyingPublication,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (uiState.editingDocumentUri != null) "Update" else "Publish")
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

@Composable
private fun DocumentPickerDialog(
    publications: List<PublicationItem>,
    selectedPublication: PublicationItem?,
    onSelectDocument: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    data class DocumentItem(val uri: String, val title: String)

    var selectedPub by remember { mutableStateOf<PublicationItem?>(selectedPublication) }
    var documents by remember { mutableStateOf<List<DocumentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedPub) {
        val pub = selectedPub ?: return@LaunchedEffect
        isLoading = true
        error = null
        try {
            val client = okhttp3.OkHttpClient()
            val url = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_LIST_RECORDS}?repo=${pub.did}&collection=${CollectionNsids.DOCUMENT}&limit=25"
            val request = okhttp3.Request.Builder().url(url).get().build()
            val body = client.newCall(request).execute().body?.string() ?: return@LaunchedEffect
            val response = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
            val records = response["records"]?.jsonArray.orEmpty()
            documents = records.mapNotNull { record ->
                val uri = record.jsonObject["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val value = record.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
                val title = value["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled"
                DocumentItem(uri, title)
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select a document to edit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                if (isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                } else if (documents.isEmpty()) {
                    Text("No documents found.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(documents) { doc ->
                            TextButton(
                                onClick = { onSelectDocument(doc.uri) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(doc.title)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
