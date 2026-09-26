package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uk.ewancroft.inkwell.R

/** Callbacks the metadata section raises; kept separate so the section stays stateless. */
data class WriterMetadataActions(
    val onAddTag: (String) -> Boolean,
    val onRemoveTag: (String) -> Unit,
    val onAddContributor: (did: String, role: String, displayName: String) -> Boolean,
    val onRemoveContributor: (WriterContributorDraft) -> Unit,
    val onToggleLabel: (String) -> Unit,
    val onBskyPostUriChanged: (String) -> Unit,
    val onCoverPicked: (ImageUploadSanitizer.Output) -> Unit,
    val onRemoveCover: () -> Unit,
    val onError: (String) -> Unit,
)

fun WriterViewModel.metadataActions(): WriterMetadataActions = WriterMetadataActions(
    onAddTag = this::addTag,
    onRemoveTag = this::removeTag,
    onAddContributor = this::addContributor,
    onRemoveContributor = this::removeContributor,
    onToggleLabel = this::toggleLabel,
    onBskyPostUriChanged = this::onBskyPostUriChanged,
    onCoverPicked = { uploadCoverImage(it.bytes, it.mimeType) },
    onRemoveCover = this::removeCoverImage,
    onError = this::setMetadataError,
)

/**
 * Collapsible editor for a document's Standard.site metadata layer: tags,
 * contributors, cover image, Bluesky discussion post, and content warnings.
 */
@Composable
fun WriterMetadataSection(
    metadata: WriterMetadataDraft,
    isUploadingCover: Boolean,
    metadataError: String?,
    enabled: Boolean,
    actions: WriterMetadataActions,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val toggleLabel = stringResource(
        if (expanded) R.string.writer_metadata_collapse else R.string.writer_metadata_expand,
    )

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = toggleLabel, role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.writer_metadata_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                if (!expanded) {
                    Text(
                        metadataSummary(metadata),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = toggleLabel,
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TagsEditor(metadata.tags, enabled, actions.onAddTag, actions.onRemoveTag)
                HorizontalDivider()
                ContributorsEditor(
                    metadata.contributors,
                    enabled,
                    actions.onAddContributor,
                    actions.onRemoveContributor,
                )
                HorizontalDivider()
                CoverImageEditor(
                    coverImage = metadata.coverImage,
                    isUploading = isUploadingCover,
                    enabled = enabled,
                    onPicked = actions.onCoverPicked,
                    onRemove = actions.onRemoveCover,
                    onError = actions.onError,
                )
                HorizontalDivider()
                BskyPostRefField(metadata.bskyPostUri, enabled, actions.onBskyPostUriChanged)
                HorizontalDivider()
                ContentWarningsPicker(metadata.labels, enabled, actions.onToggleLabel)
            }
        }

        if (metadataError != null) {
            Text(
                metadataError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** One-line description of what's set, so a collapsed section still says something true. */
@Composable
private fun metadataSummary(metadata: WriterMetadataDraft): String {
    val parts = buildList {
        if (metadata.tags.isNotEmpty()) {
            add(pluralStringResource(R.plurals.writer_metadata_summary_tags, metadata.tags.size, metadata.tags.size))
        }
        if (metadata.contributors.isNotEmpty()) {
            val count = metadata.contributors.size
            add(pluralStringResource(R.plurals.writer_metadata_summary_contributors, count, count))
        }
        if (metadata.coverImage != null) add(stringResource(R.string.writer_metadata_summary_cover))
        if (metadata.bskyPostUri.isNotBlank()) add(stringResource(R.string.writer_metadata_summary_bsky))
        if (metadata.labels.isNotEmpty()) {
            add(pluralStringResource(R.plurals.writer_metadata_summary_warnings, metadata.labels.size, metadata.labels.size))
        }
    }
    return if (parts.isEmpty()) stringResource(R.string.writer_metadata_summary_none) else parts.joinToString(" · ")
}
