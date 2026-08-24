package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
import uk.ewancroft.inkwell.util.rememberInkwellHaptics

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

    // Local TextFieldValue tracks cursor/selection alongside the text, so
    // FormattingToolbar can insert markdown at the actual cursor instead of
    // always appending to the end. viewModel.markdown stays the source of
    // truth for anything outside this editor (preview, publish, loss
    // reporting) — this mirrors it, syncing text changes back down, and
    // resyncing (with the cursor reset) only when the editor loads a
    // different document's content rather than on every keystroke.
    var markdownField by remember { mutableStateOf(TextFieldValue(uiState.markdown)) }
    LaunchedEffect(uiState.markdown) {
        // Only an *external* change (document load, image-upload markdown
        // insertion) lands here — our own edits already match, since
        // updateMarkdownField updates both in the same call.
        if (uiState.markdown != markdownField.text) {
            markdownField = TextFieldValue(uiState.markdown, TextRange(uiState.markdown.length))
        }
    }
    fun updateMarkdownField(value: TextFieldValue) {
        markdownField = value
        if (value.text != uiState.markdown) viewModel.onMarkdownChanged(value.text)
    }

    val formats = listOf("Leaflet", "Markpub", "pckt", "Offprint")

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember { uk.ewancroft.inkwell.util.appVersionString(context) }
    val haptics = rememberInkwellHaptics()
    LaunchedEffect(uiState.publishSuccess) {
        if (uiState.publishSuccess != null) haptics.success()
    }
    LaunchedEffect(uiState.publishError) {
        if (uiState.publishError != null) haptics.error()
    }

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
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
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
                textFieldValue = markdownField,
                onTextFieldValueChange = ::updateMarkdownField,
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
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = uiState.showPreview,
                    onCheckedChange = { viewModel.togglePreview() },
                )
            }

            // Content editor
            OutlinedTextField(
                value = markdownField, onValueChange = ::updateMarkdownField,
                label = { Text("Content (Markdown)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp),
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
                onClick = {
                    haptics.medium()
                    viewModel.publish()
                },
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

            if (showCredits) {
                CreditsView(
                    appVersion = appVersion,
                    onSignOut = onSignOut,
                    onDismiss = { showCredits = false },
                )
            }

            if (uiState.showCreateDialog) {
                CreatePublicationDialog(
                    uiState = uiState,
                    onUrlChanged = { viewModel.onCreateUrlChanged(it) },
                    onNameChanged = { viewModel.onCreateNameChanged(it) },
                    onDescriptionChanged = { viewModel.onCreateDescriptionChanged(it) },
                    onCreate = { viewModel.createPublication() },
                    onDismiss = { viewModel.dismissCreateDialog() },
                )
            }
        }
    }
