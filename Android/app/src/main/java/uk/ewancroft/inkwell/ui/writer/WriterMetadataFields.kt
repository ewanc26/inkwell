package uk.ewancroft.inkwell.ui.writer

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.shared.model.DocumentMetadata

@Composable
private fun FieldHeading(textRes: Int) {
    Text(stringResource(textRes), style = MaterialTheme.typography.labelLarge)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagsEditor(
    tags: List<String>,
    enabled: Boolean,
    onAdd: (String) -> Boolean,
    onRemove: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val submit: () -> Unit = { if (onAdd(input)) input = "" }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeading(R.string.writer_tags)
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            enabled = enabled,
            label = { Text(stringResource(R.string.writer_tag_input)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            trailingIcon = {
                IconButton(onClick = submit, enabled = enabled && input.isNotBlank()) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.writer_add_tag))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    val removeLabel = stringResource(R.string.writer_remove_tag, tag)
                    InputChip(
                        selected = false,
                        enabled = enabled,
                        onClick = { onRemove(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(Icons.Outlined.Close, contentDescription = null, Modifier.size(InputChipDefaults.IconSize))
                        },
                        modifier = Modifier.semantics { contentDescription = removeLabel },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ContributorsEditor(
    contributors: List<WriterContributorDraft>,
    enabled: Boolean,
    onAdd: (did: String, role: String, displayName: String) -> Boolean,
    onRemove: (WriterContributorDraft) -> Unit,
) {
    var did by rememberSaveable { mutableStateOf("") }
    var role by rememberSaveable { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeading(R.string.writer_contributors)
        contributors.forEach { contributor ->
            val name = contributor.displayName ?: contributor.did
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    val detail = listOfNotNull(contributor.role, contributor.did.takeIf { contributor.displayName != null })
                    if (detail.isNotEmpty()) {
                        Text(
                            detail.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { onRemove(contributor) }, enabled = enabled) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.writer_remove_contributor, name),
                    )
                }
            }
        }
        OutlinedTextField(
            value = did,
            onValueChange = { did = it },
            enabled = enabled,
            label = { Text(stringResource(R.string.writer_contributor_did)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = role,
            onValueChange = { role = it },
            enabled = enabled,
            label = { Text(stringResource(R.string.writer_contributor_role)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            enabled = enabled,
            label = { Text(stringResource(R.string.writer_contributor_display_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = {
                if (onAdd(did, role, displayName)) {
                    did = ""
                    role = ""
                    displayName = ""
                }
            },
            enabled = enabled && did.isNotBlank(),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.writer_add_contributor))
        }
    }
}

@Composable
internal fun CoverImageEditor(
    coverImage: JsonObject?,
    isUploading: Boolean,
    enabled: Boolean,
    onPicked: (ImageUploadSanitizer.Output) -> Unit,
    onRemove: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        when (val picked = readPickedImage(context, uri)) {
            PickedImageBytes.Unreadable -> onError("The selected image could not be read.")
            PickedImageBytes.TooLarge -> onError("Image is too large. Choose an image no larger than 10 MiB.")
            is PickedImageBytes.Read ->
                runCatching { ImageUploadSanitizer.sanitize(picked.bytes, DocumentMetadata.COVER_IMAGE_MAX_BYTES) }
                    .onSuccess(onPicked)
                    .onFailure { onError(it.localizedMessage ?: "The image could not be prepared for upload.") }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeading(R.string.writer_cover_image)
        when {
            isUploading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.writer_cover_uploading), style = MaterialTheme.typography.bodySmall)
            }
            coverImage != null -> {
                val mimeType = coverImage["mimeType"]?.jsonPrimitive?.contentOrNull ?: "image"
                val size = coverImage["size"]?.jsonPrimitive?.longOrNull
                    ?.let { Formatter.formatShortFileSize(context, it) } ?: "?"
                Text(
                    stringResource(R.string.writer_cover_attached, mimeType, size),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch("image/*") }, enabled = enabled) {
                        Text(stringResource(R.string.writer_cover_replace))
                    }
                    TextButton(onClick = onRemove, enabled = enabled) {
                        Text(stringResource(R.string.writer_cover_remove))
                    }
                }
            }
            else -> OutlinedButton(onClick = { picker.launch("image/*") }, enabled = enabled) {
                Icon(Icons.Outlined.Image, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.writer_cover_choose))
            }
        }
        Text(
            stringResource(R.string.writer_cover_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun BskyPostRefField(uri: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = uri,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(stringResource(R.string.writer_bsky_post_ref)) },
        supportingText = { Text(stringResource(R.string.writer_bsky_post_ref_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ContentWarningsPicker(selected: List<String>, enabled: Boolean, onToggle: (String) -> Unit) {
    // Values already on the record but outside the offered set stay visible
    // (and removable) instead of being silently kept or silently dropped.
    val values = (DocumentMetadata.SELF_LABEL_VALUES + selected).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeading(R.string.writer_content_warnings)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            values.forEach { value ->
                val isSelected = value in selected
                FilterChip(
                    selected = isSelected,
                    enabled = enabled,
                    onClick = { onToggle(value) },
                    label = { Text(selfLabelTitle(value)) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Outlined.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun selfLabelTitle(value: String): String = when (value) {
    "sexual" -> stringResource(R.string.writer_label_sexual)
    "nudity" -> stringResource(R.string.writer_label_nudity)
    "porn" -> stringResource(R.string.writer_label_porn)
    "graphic-media" -> stringResource(R.string.writer_label_graphic_media)
    else -> value
}
