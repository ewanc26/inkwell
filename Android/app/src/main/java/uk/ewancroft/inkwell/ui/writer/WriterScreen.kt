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
import androidx.compose.ui.res.stringResource
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.ui.components.CreditsView
import androidx.hilt.navigation.compose.hiltViewModel
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
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var pubExpanded by remember { mutableStateOf(false) }
    var formatExpanded by remember { mutableStateOf(false) }
    var showCredits by remember { mutableStateOf(false) }
    var showDocumentPicker by remember { mutableStateOf(false) }
    var pendingImage by remember { mutableStateOf<ImageUploadSanitizer.Output?>(null) }
    var imageAltText by rememberSaveable { mutableStateOf("") }
    var imageIsDecorative by rememberSaveable { mutableStateOf(false) }

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
        when (val picked = readPickedImage(context, uri)) {
            PickedImageBytes.Unreadable -> Unit
            PickedImageBytes.TooLarge ->
                viewModel.setPublishError("Image is too large. Choose an image no larger than 10 MiB.")
            is PickedImageBytes.Read -> runCatching { ImageUploadSanitizer.sanitize(picked.bytes) }
                .onSuccess {
                    pendingImage = it
                    imageAltText = ""
                    imageIsDecorative = false
                }
                .onFailure { viewModel.setPublishError(it.localizedMessage ?: "The image could not be prepared for upload.") }
        }
    }

    pendingImage?.let { image ->
        AlertDialog(
            onDismissRequest = { pendingImage = null },
            title = { Text(stringResource(R.string.writer_describe_image)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.writer_alt_text_guidance))
                    OutlinedTextField(
                        value = imageAltText,
                        onValueChange = { imageAltText = it },
                        enabled = !imageIsDecorative,
                        label = { Text(stringResource(R.string.writer_alt_text)) },
                        supportingText = { Text(stringResource(R.string.writer_alt_text_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = imageIsDecorative,
                            onCheckedChange = { imageIsDecorative = it },
                        )
                        Text(stringResource(R.string.writer_decorative_image))
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = imageIsDecorative || imageAltText.isNotBlank(),
                    onClick = {
                        viewModel.uploadImage(
                            image.bytes,
                            image.mimeType,
                            if (imageIsDecorative) "" else imageAltText.trim(),
                        )
                        pendingImage = null
                    },
                ) { Text(stringResource(R.string.writer_insert_image)) }
            },
            dismissButton = { TextButton(onClick = { pendingImage = null }) { Text(stringResource(R.string.reader_cancel)) } },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadPublications()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.writer_write_title)) },
                navigationIcon = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = stringResource(R.string.writer_sign_out),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCredits = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = stringResource(R.string.writer_about))
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
                Text(stringResource(R.string.writer_no_publications), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { viewModel.showCreateDialog() }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.writer_create_publication))
                }
            } else {
                // Publication picker
                Box {
                    OutlinedButton(
                        onClick = { pubExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            uiState.selectedPublication?.name ?: stringResource(R.string.writer_select_publication),
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
                    Text(stringResource(R.string.writer_new_publication))
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
                        stringResource(R.string.writer_verifying_publication),
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
                            stringResource(R.string.writer_editing_existing_document),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { viewModel.cancelEditing() }) {
                            Text(stringResource(R.string.reader_cancel), style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            enabled = !uiState.isPublishing,
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.writer_delete_document))
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
                    Text(stringResource(if (uiState.editingDocumentUri != null) R.string.writer_change_document else R.string.writer_edit_existing_document))
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
                label = { Text(stringResource(R.string.writer_title)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = uiState.description, onValueChange = { viewModel.onDescriptionChanged(it) },
                label = { Text(stringResource(R.string.writer_description_optional)) }, maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.path, onValueChange = { viewModel.onPathChanged(it) },
                label = { Text(stringResource(R.string.writer_path_optional)) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            WriterMetadataSection(
                metadata = uiState.metadata,
                isUploadingCover = uiState.isUploadingCover,
                metadataError = uiState.metadataError,
                enabled = !uiState.isPublishing,
                actions = remember(viewModel) { viewModel.metadataActions() },
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
                    stringResource(R.string.writer_preview),
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
                label = { Text(stringResource(R.string.writer_content_markdown)) },
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
                            Text(stringResource(R.string.writer_view_post))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null, Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text(stringResource(R.string.writer_delete_title, uiState.title)) },
                    text = { Text(stringResource(R.string.writer_delete_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirmation = false
                                viewModel.deleteDocument()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) { Text(stringResource(R.string.writer_delete)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) { Text(stringResource(R.string.reader_cancel)) }
                    },
                )
            }

            Button(
                onClick = {
                    haptics.medium()
                    viewModel.publish()
                },
                enabled = uiState.title.isNotBlank() && uiState.selectedPublication != null && uiState.verifiedPublicationUri != null && !uiState.isPublishing && !uiState.isVerifyingPublication && !uiState.isUploadingCover,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (uiState.editingDocumentUri != null) R.string.writer_update else R.string.writer_publish))
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
