package uk.ewancroft.inkwell.ui.writer

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.draft.WriterDraftPolicy
import uk.ewancroft.inkwell.shared.draft.WriterDraftSchema

private val draftJson = Json { ignoreUnknownKeys = true }
private val contributorsSerializer = ListSerializer(WriterContributorDraft.serializer())

/**
 * The autosaveable projection of the Writer, with an empty `savedAt` so two
 * projections compare equal whenever the user-visible content does.
 */
internal fun WriterUiState.toDraft(accountDid: String): WriterDraftSchema = WriterDraftSchema(
    accountDid = accountDid,
    publicationUri = selectedPublication?.uri.orEmpty(),
    editingDocumentUri = editingDocumentUri,
    editingDocumentRevision = editingDocumentUri?.let { editingDocumentRecordCID },
    title = title,
    description = description,
    path = path,
    format = selectedFormat,
    markdown = markdown,
    uploadedBlobKeys = uploadedBlobs.keys.sorted(),
    metadataTags = metadata.tags,
    metadataContributors = draftJson.encodeToString(contributorsSerializer, metadata.contributors),
    bskyPostRefUri = metadata.bskyPostUri.takeIf(String::isNotBlank),
    selfLabelValues = metadata.labels,
    coverImageJson = metadata.coverImage?.let { draftJson.encodeToString(JsonObject.serializer(), it) },
    savedAt = "",
)

/** [draft]'s content as a Writer metadata draft; the Bluesky post CID is re-resolved at publish. */
private fun WriterDraftSchema.toMetadata(): WriterMetadataDraft = WriterMetadataDraft(
    tags = metadataTags,
    contributors = runCatching { draftJson.decodeFromString(contributorsSerializer, metadataContributors) }
        .getOrDefault(emptyList()),
    labels = selfLabelValues,
    coverImage = coverImageJson?.let { runCatching { draftJson.decodeFromString(JsonObject.serializer(), it) }.getOrNull() },
    bskyPostUri = bskyPostRefUri.orEmpty(),
)

/**
 * Loads this account's draft (if any) on launch, then starts autosaving.
 *
 * Autosave only begins once the restore has settled, so the empty initial
 * editor can never overwrite a draft that has not been read yet.
 */
internal fun WriterViewModel.restoreDraftAndStartAutosave() {
    viewModelScope.launch {
        val did = pdsRepository.getSession()?.did?.takeIf(String::isNotBlank) ?: return@launch
        accountDid = did
        val draft = draftStore.load(did)
        val editingUri = draft?.editingDocumentUri
        when {
            draft == null -> Unit
            // An edit needs the live record (for its CID and unmodelled fields);
            // loading it compares revisions and applies or holds the draft.
            editingUri != null -> loadDocumentForEditingNow(editingUri)
            else -> applyDraft(draft)
        }
        startDraftAutosave(did)
    }
}

private fun WriterViewModel.startDraftAutosave(did: String) {
    viewModelScope.launch {
        uiState.map { it.toDraft(did) to (it.draftConflict != null) }
            .distinctUntilChanged()
            .collectLatest { (fields, conflictPending) ->
                // Never write while the user is choosing between two versions.
                if (conflictPending) return@collectLatest
                delay(WriterDraftPolicy.MAX_DEBOUNCE_MS)
                when {
                    fields == draftBaseline -> draftStore.clear(did)
                    // Empty only clears once this session has written: an editor
                    // that never loaded (offline edit restore) must not erase the draft.
                    WriterDraftPolicy.isEmpty(fields) -> if (draftWritten) draftStore.clear(did)
                    else -> {
                        draftStore.save(fields.copy(savedAt = java.time.Instant.now().toString()))
                        draftWritten = true
                    }
                }
            }
    }
}

/** Puts [draft]'s content into the editor on top of whatever document is loaded. */
internal fun WriterViewModel.applyDraft(draft: WriterDraftSchema) {
    val state = uiStateInternal.value
    val publication = state.publications.firstOrNull { it.uri == draft.publicationUri }
    if (publication == null && draft.publicationUri.isNotBlank()) pendingDraftPublicationUri = draft.publicationUri
    uiStateInternal.value = state.copy(
        title = draft.title,
        description = draft.description,
        path = draft.path,
        markdown = draft.markdown,
        selectedFormat = draft.format.ifBlank { state.selectedFormat },
        uploadedBlobs = state.uploadedBlobs + draft.uploadedBlobKeys.associateWith(::blobRefStub),
        draftRestored = true,
        showDraftBanner = true,
        draftConflict = null,
    )
    setMetadata(draft.toMetadata())
    publication?.let(::selectPublication)
}

/** Sets the content that counts as "nothing to save", e.g. a freshly loaded or just-published document. */
internal fun WriterViewModel.markDraftSettled() {
    val did = accountDid ?: return
    draftBaseline = uiStateInternal.value.toDraft(did)
}

/** Clears the stored draft after a successful publish or update. */
internal fun WriterViewModel.onDraftPublished() {
    markDraftSettled()
    uiStateInternal.value = uiStateInternal.value.copy(draftRestored = false, showDraftBanner = false)
    val did = accountDid ?: return
    viewModelScope.launch { draftStore.clear(did) }
}

/**
 * Throws the restored draft away: an edit reverts to the published document,
 * a new document goes back to an empty editor.
 */
fun WriterViewModel.discardDraft() {
    val state = uiStateInternal.value
    uiStateInternal.value = if (state.editingDocumentUri != null) {
        state.copy(
            title = state.editingDocumentTitle.orEmpty(),
            description = state.editingDocumentDescription.orEmpty(),
            path = state.editingDocumentPath.orEmpty(),
            markdown = state.editingDocumentMarkdown.orEmpty(),
        )
    } else {
        state.copy(title = "", description = "", path = "", markdown = "", uploadedBlobs = emptyMap())
    }.copy(draftRestored = false, showDraftBanner = false, draftConflict = null)
    setMetadata(state.editingDocumentRecord?.let(WriterMetadataDraft::fromRecord) ?: WriterMetadataDraft())
    onDraftPublished()
}

fun WriterViewModel.dismissDraftBanner() {
    uiStateInternal.value = uiStateInternal.value.copy(showDraftBanner = false)
}

/**
 * Resolves a draft whose document changed on the PDS since it was saved.
 *
 * Keeping the draft rebases it onto the current revision, so publishing
 * deliberately replaces the remote edit (still guarded by `swapRecord`).
 * Using the published version drops the draft.
 */
fun WriterViewModel.resolveDraftConflict(keepDraft: Boolean) {
    val conflict = uiStateInternal.value.draftConflict ?: return
    if (keepDraft) applyDraft(conflict) else discardDraft()
}

internal fun blobRefStub(link: String): JsonObject = buildJsonObject { put("\$link", link) }
