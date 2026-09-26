package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import uk.ewancroft.inkwell.shared.model.DocumentMetadata
import uk.ewancroft.inkwell.shared.validation.StandardSiteValidation

private const val METADATA_DRAFT_KEY = "writer.metadataDraft"

private val draftJson = Json { ignoreUnknownKeys = true }

/** Restores a metadata draft parked by [setMetadata] before process death. */
internal fun WriterViewModel.restoreMetadataDraft() {
    val saved = savedStateHandle.get<String>(METADATA_DRAFT_KEY) ?: return
    val draft = runCatching {
        draftJson.decodeFromString(WriterMetadataDraft.serializer(), saved)
    }.getOrNull() ?: return
    uiStateInternal.value = uiStateInternal.value.copy(metadata = draft)
}

/** Replaces the metadata draft and parks it in `SavedStateHandle`. */
internal fun WriterViewModel.setMetadata(draft: WriterMetadataDraft) {
    uiStateInternal.value = uiStateInternal.value.copy(metadata = draft, metadataError = null)
    savedStateHandle[METADATA_DRAFT_KEY] =
        draftJson.encodeToString(WriterMetadataDraft.serializer(), draft)
}

/** The first shared-validation error for [metadata], formatted for display. */
private fun firstMetadataError(metadata: DocumentMetadata): String? =
    StandardSiteValidation.validateMetadata(metadata).firstOrNull()?.let { "${it.field}: ${it.message}" }

private fun WriterViewModel.rejectMetadata(message: String) {
    uiStateInternal.value = uiStateInternal.value.copy(metadataError = message)
}

/** Adds [tag]; returns false (with [WriterUiState.metadataError] set) when rejected. */
fun WriterViewModel.addTag(tag: String): Boolean {
    val trimmed = tag.trim()
    if (trimmed.isEmpty()) return false
    val current = uiStateInternal.value.metadata
    if (trimmed in current.tags) return true
    firstMetadataError(DocumentMetadata(tags = listOf(trimmed)))?.let {
        rejectMetadata(it)
        return false
    }
    setMetadata(current.copy(tags = current.tags + trimmed))
    return true
}

fun WriterViewModel.removeTag(tag: String) {
    val current = uiStateInternal.value.metadata
    setMetadata(current.copy(tags = current.tags - tag))
}

/** Adds a contributor; returns false (with [WriterUiState.metadataError] set) when rejected. */
fun WriterViewModel.addContributor(did: String, role: String, displayName: String): Boolean {
    val contributor = WriterContributorDraft(
        did = did.trim(),
        role = role.trim().ifEmpty { null },
        displayName = displayName.trim().ifEmpty { null },
    )
    val candidate = DocumentMetadata(
        contributors = listOf(
            DocumentMetadata.Contributor(contributor.did, contributor.role, contributor.displayName),
        ),
    )
    firstMetadataError(candidate)?.let {
        rejectMetadata(it)
        return false
    }
    val current = uiStateInternal.value.metadata
    if (current.contributors.any { it.did == contributor.did }) {
        rejectMetadata("${contributor.did} is already a contributor")
        return false
    }
    setMetadata(current.copy(contributors = current.contributors + contributor))
    return true
}

fun WriterViewModel.removeContributor(contributor: WriterContributorDraft) {
    val current = uiStateInternal.value.metadata
    setMetadata(current.copy(contributors = current.contributors - contributor))
}

fun WriterViewModel.toggleLabel(value: String) {
    val current = uiStateInternal.value.metadata
    val labels = if (value in current.labels) current.labels - value else current.labels + value
    setMetadata(current.copy(labels = labels))
}

fun WriterViewModel.onBskyPostUriChanged(uri: String) {
    val current = uiStateInternal.value.metadata
    // The known CID belongs to the old URI; a different post needs resolving again.
    val cid = current.bskyPostCid.takeIf { uri.trim() == current.bskyPostUri.trim() }
    setMetadata(current.copy(bskyPostUri = uri, bskyPostCid = cid))
}

fun WriterViewModel.uploadCoverImage(bytes: ByteArray, mimeType: String) {
    viewModelScope.launch {
        uiStateInternal.value = uiStateInternal.value.copy(isUploadingCover = true, metadataError = null)
        try {
            val result = pdsRepository.uploadBlob(bytes, mimeType)
            val blob = result["blob"]?.jsonObject
                ?: throw IllegalStateException("Missing blob in upload response")
            uiStateInternal.value = uiStateInternal.value.copy(isUploadingCover = false)
            setMetadata(uiStateInternal.value.metadata.copy(coverImage = blob))
        } catch (e: Exception) {
            uiStateInternal.value = uiStateInternal.value.copy(
                isUploadingCover = false,
                metadataError = "Failed to upload cover image: ${e.message}",
            )
        }
    }
}

fun WriterViewModel.removeCoverImage() {
    setMetadata(uiStateInternal.value.metadata.copy(coverImage = null))
}

fun WriterViewModel.setMetadataError(message: String) {
    rejectMetadata(message)
}
