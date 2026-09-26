package uk.ewancroft.inkwell.shared.draft

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A Writer draft as autosaved to local, app-private storage on either platform.
 *
 * This is the one on-disk shape: Android writes it to DataStore and iOS to a
 * file-protected JSON file, both through [WriterDraftCodec], so a draft's
 * meaning cannot drift between platforms. It never leaves the device and
 * holds no credentials — only what the user has typed and the non-secret
 * identifiers needed to put it back in context.
 *
 * Everything is a plain string or list so it bridges to Swift without
 * generics. [metadataContributors] is a JSON array string for the same
 * reason; [coverImageJson] is the blob ref exactly as the PDS returned it.
 */
@Serializable
data class WriterDraftSchema(
    /** The signed-in DID the draft belongs to; a draft never restores into another account. */
    val accountDid: String,
    /** The selected publication's AT-URI, or empty when none was selected. */
    val publicationUri: String,
    /** The document being edited, or null for a new document. */
    val editingDocumentUri: String? = null,
    /** CID of [editingDocumentUri] when it was loaded, used to detect remote edits since. */
    val editingDocumentRevision: String? = null,
    val title: String = "",
    val description: String = "",
    val path: String = "",
    val format: String = "",
    val markdown: String = "",
    /** Blob CIDs inserted into [markdown] as images. */
    val uploadedBlobKeys: List<String> = emptyList(),
    val metadataTags: List<String> = emptyList(),
    /** JSON array of `{did, role?, displayName?}` objects. */
    val metadataContributors: String = "[]",
    val bskyPostRefUri: String? = null,
    val selfLabelValues: List<String> = emptyList(),
    /** Cover image blob ref as JSON, or null. Not in schema v1's original field list; optional so older files decode. */
    val coverImageJson: String? = null,
    /** ISO-8601 instant the draft was written. */
    val savedAt: String,
    val schemaVersion: Int = WriterDraftPolicy.SCHEMA_VERSION,
)

/** Autosave timing, expiry, and conflict rules shared by both Writers. */
object WriterDraftPolicy {
    /** Autosave waits this long after the last edit before writing. */
    const val MAX_DEBOUNCE_MS: Long = 2000L
    const val SCHEMA_VERSION: Int = 1
    /** Drafts older than this are discarded rather than offered for recovery. */
    const val MAX_AGE_MS: Long = 30L * 24 * 60 * 60 * 1000

    /**
     * True when [savedAt] is more than [MAX_AGE_MS] before [nowMs].
     *
     * An unparseable timestamp counts as stale: a draft whose age cannot be
     * established is not one to resurrect silently.
     */
    @OptIn(ExperimentalTime::class)
    fun isDraftStale(savedAt: String, nowMs: Long): Boolean {
        val savedMs = runCatching { Instant.parse(savedAt).toEpochMilliseconds() }.getOrNull() ?: return true
        return nowMs - savedMs > MAX_AGE_MS
    }

    /** True when the draft carries nothing the user typed or chose, so saving it is pointless. */
    fun isEmpty(draft: WriterDraftSchema): Boolean =
        draft.title.isBlank() &&
            draft.description.isBlank() &&
            draft.path.isBlank() &&
            draft.markdown.isBlank() &&
            draft.metadataTags.isEmpty() &&
            (draft.metadataContributors.isBlank() || draft.metadataContributors.trim() == "[]") &&
            draft.bskyPostRefUri.isNullOrBlank() &&
            draft.selfLabelValues.isEmpty() &&
            draft.coverImageJson == null

    /** True when [draft] may be offered to [accountDid] at [nowMs]. */
    fun isRestorable(draft: WriterDraftSchema, accountDid: String, nowMs: Long): Boolean =
        draft.accountDid == accountDid &&
            draft.schemaVersion == SCHEMA_VERSION &&
            !isDraftStale(draft.savedAt, nowMs)

    /**
     * True when [draft] edits [documentUri] but was based on a different
     * revision than the [remoteRevision] now on the PDS — the document changed
     * elsewhere, so loading either version silently would lose the other.
     */
    fun hasRevisionConflict(draft: WriterDraftSchema, documentUri: String, remoteRevision: String): Boolean =
        draft.editingDocumentUri == documentUri &&
            draft.editingDocumentRevision != null &&
            draft.editingDocumentRevision != remoteRevision
}

/** JSON encoding for [WriterDraftSchema], shared so both platforms read each other's rules identically. */
object WriterDraftCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(draft: WriterDraftSchema): String = json.encodeToString(WriterDraftSchema.serializer(), draft)

    /** Decodes [raw], or returns null when it is malformed or from an unknown schema version. */
    fun decode(raw: String): WriterDraftSchema? =
        runCatching { json.decodeFromString(WriterDraftSchema.serializer(), raw) }
            .getOrNull()
            ?.takeIf { it.schemaVersion == WriterDraftPolicy.SCHEMA_VERSION }
}
