package uk.ewancroft.inkwell.ui.moderation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import uk.ewancroft.inkwell.util.LabelMode
import uk.ewancroft.inkwell.util.ModerationPreferences

/**
 * A focused settings surface for reader content warnings. The shared KMP
 * filter makes the actual visibility decision; this dialog owns only the
 * Android-native preference controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationSettingsDialog(
    onDismiss: () -> Unit,
    onPreferencesChanged: () -> Unit,
) {
    val context = LocalContext.current
    var revision by remember { mutableIntStateOf(0) }
    var customLabelInput by remember { mutableStateOf("") }
    var labelerInput by remember { mutableStateOf("") }
    var keywordInput by remember { mutableStateOf("") }

    val customLabels = remember(revision) { ModerationPreferences.customLabels(context).sorted() }
    val labelers = remember(revision) { ModerationPreferences.knownLabelers(context).sorted() }
    val disabledLabelers = remember(revision) { ModerationPreferences.disabledLabelers(context) }
    val hiddenKeywords = remember(revision) { ModerationPreferences.hiddenKeywords(context).sorted() }

    fun changed() {
        revision += 1
        onPreferencesChanged()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column {
                TopAppBar(
                    title = { Text("Content filters") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    FilterSectionTitle("Content warnings")
                    Text(
                        "Choose whether a labelled article is shown, shown behind a warning, or hidden until you reveal it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    standardLabels.forEach { label ->
                        LabelModeRow(
                            title = label.title,
                            mode = ModerationPreferences.labelMode(context, label.value),
                            onModeSelected = { mode ->
                                ModerationPreferences.setLabelMode(context, label.value, mode)
                                changed()
                            },
                        )
                    }

                    HorizontalDivider()

                    FilterSectionTitle("Custom labels")
                    Text(
                        "Add labels used by a publication or labeler service, then choose how Inkwell should display them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AddValueRow(
                        value = customLabelInput,
                        onValueChange = { customLabelInput = it },
                        label = "Custom label",
                        addLabel = "Add label",
                        onAdd = {
                            ModerationPreferences.addCustomLabel(context, customLabelInput)
                            customLabelInput = ""
                            changed()
                        },
                    )
                    customLabels.forEach { label ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = {
                                        ModerationPreferences.removeCustomLabel(context, label)
                                        changed()
                                    },
                                ) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove $label")
                                }
                            }
                            LabelModeRow(
                                title = "How to show $label",
                                mode = ModerationPreferences.labelMode(context, label),
                                onModeSelected = { mode ->
                                    ModerationPreferences.setLabelMode(context, label, mode)
                                    changed()
                                },
                                showTitle = false,
                            )
                        }
                    }

                    HorizontalDivider()

                    FilterSectionTitle("Labeler services")
                    Text(
                        "Disable a source to ignore labels from that service. This applies whenever the PDS includes a label source with an article.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AddValueRow(
                        value = labelerInput,
                        onValueChange = { labelerInput = it },
                        label = "Labeler DID or service",
                        addLabel = "Add labeler",
                        onAdd = {
                            ModerationPreferences.setLabelerEnabled(context, labelerInput, enabled = true)
                            labelerInput = ""
                            changed()
                        },
                    )
                    labelers.forEach { labeler ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(labeler, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(
                                checked = labeler !in disabledLabelers,
                                onCheckedChange = { enabled ->
                                    ModerationPreferences.setLabelerEnabled(context, labeler, enabled)
                                    changed()
                                },
                            )
                            Spacer(Modifier.width(4.dp))
                            IconButton(
                                onClick = {
                                    ModerationPreferences.removeLabeler(context, labeler)
                                    changed()
                                },
                            ) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove $labeler")
                            }
                        }
                    }

                    HorizontalDivider()

                    FilterSectionTitle("Keywords to hide")
                    Text(
                        "Keywords are matched case-insensitively against article titles, summaries, and available text.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AddValueRow(
                        value = keywordInput,
                        onValueChange = { keywordInput = it },
                        label = "Keyword",
                        addLabel = "Add keyword",
                        onAdd = {
                            ModerationPreferences.addKeyword(context, keywordInput)
                            keywordInput = ""
                            changed()
                        },
                    )
                    hiddenKeywords.forEach { keyword ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(keyword, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    ModerationPreferences.removeKeyword(context, keyword)
                                    changed()
                                },
                            ) { Text("Remove") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun LabelModeRow(
    title: String,
    mode: LabelMode,
    onModeSelected: (LabelMode) -> Unit,
    showTitle: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showTitle) Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            LabelMode.entries.forEach { candidate ->
                FilterChip(
                    selected = mode == candidate,
                    onClick = { onModeSelected(candidate) },
                    label = { Text(candidate.displayName) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AddValueRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    addLabel: String,
    onAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        TextButton(onClick = onAdd, enabled = value.isNotBlank()) { Text(addLabel) }
    }
}

private data class StandardLabel(val value: String, val title: String)

private val standardLabels = listOf(
    StandardLabel("nsfw", "Explicit content"),
    StandardLabel("sexual", "Sexual content"),
    StandardLabel("gore", "Graphic content"),
    StandardLabel("self-harm", "Self-harm"),
    StandardLabel("impersonation", "Impersonation"),
)

private val LabelMode.displayName: String
    get() = when (this) {
        LabelMode.Show -> "Show"
        LabelMode.Warn -> "Warn"
        LabelMode.Hide -> "Hide"
    }
