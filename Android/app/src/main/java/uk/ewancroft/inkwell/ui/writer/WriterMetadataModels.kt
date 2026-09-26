package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import uk.ewancroft.inkwell.shared.content.JsonMapBridge
import uk.ewancroft.inkwell.shared.model.DocumentMetadata

/** One `site.standard.document#contributor` as the Writer edits it. */
@Serializable
data class WriterContributorDraft(
    val did: String,
    val role: String? = null,
    val displayName: String? = null,
)

/**
 * The editable subset of a document's Standard.site metadata layer.
 *
 * `@Serializable` so the ViewModel can park it in `SavedStateHandle` as a JSON
 * string and restore it after process death. The wire rules (trimming, empty
 * means absent, canonical shapes) stay in shared [DocumentMetadata]; this is
 * only the draft the user is building.
 *
 * `links` is deliberately not here: the Writer has no UI for the open `links`
 * union, so an edited record keeps its existing `links` value byte-for-byte
 * rather than round-tripping it through a model that might re-shape it.
 */
@Serializable
data class WriterMetadataDraft(
    val tags: List<String> = emptyList(),
    val contributors: List<WriterContributorDraft> = emptyList(),
    val labels: List<String> = emptyList(),
    /** The uploaded blob ref exactly as the PDS returned it (or as the record held it). */
    val coverImage: JsonObject? = null,
    val bskyPostUri: String = "",
    /**
     * CID of the post at [bskyPostUri], once known. A `strongRef` requires
     * both, so it is resolved at publish time whenever the URI has changed.
     */
    val bskyPostCid: String? = null,
) {
    /**
     * The shared-model view of this draft, used for validation and for the
     * wire shapes. Cover image and links are written separately — see
     * [applyDocumentMetadata].
     */
    fun toDocumentMetadata(): DocumentMetadata = DocumentMetadata(
        tags = tags,
        contributors = contributors.map {
            DocumentMetadata.Contributor(did = it.did, role = it.role, displayName = it.displayName)
        },
        labels = labels,
        bskyPostRef = bskyPostUri.trim().takeIf(String::isNotEmpty)
            ?.let { DocumentMetadata.BskyPostRef(uri = it, cid = bskyPostCid) },
    )

    companion object {
        /** Builds a draft from a document record's `value` as loaded for editing. */
        fun fromRecord(value: JsonObject): WriterMetadataDraft {
            val metadata = DocumentMetadata.read(JsonMapBridge.jsonToMap(value))
            return WriterMetadataDraft(
                tags = metadata.tags,
                contributors = metadata.contributors.map {
                    WriterContributorDraft(did = it.did, role = it.role, displayName = it.displayName)
                },
                labels = metadata.labels,
                // Taken from the JSON directly rather than via the Map bridge,
                // so the blob ref is written back exactly as it was read.
                coverImage = runCatching { value["coverImage"]?.jsonObject }.getOrNull(),
                bskyPostUri = metadata.bskyPostRef?.uri.orEmpty(),
                bskyPostCid = metadata.bskyPostRef?.cid,
            )
        }
    }
}

/** A metadata value that could not be turned into a valid record field. */
internal class WriterMetadataException(message: String) : IllegalStateException(message)

/**
 * Fills in [WriterMetadataDraft.bskyPostCid] when a Bluesky post URI is set
 * without one, since `com.atproto.repo.strongRef` requires both.
 *
 * An unresolvable post is an error rather than a CID-less ref: writing only
 * the URI would produce a record that fails Lexicon validation elsewhere.
 */
internal suspend fun resolveBskyPostRef(
    draft: WriterMetadataDraft,
    fetchCid: suspend (String) -> String?,
): WriterMetadataDraft {
    val uri = draft.bskyPostUri.trim()
    if (uri.isEmpty()) return draft.copy(bskyPostUri = "", bskyPostCid = null)
    if (draft.bskyPostCid != null) return draft.copy(bskyPostUri = uri)
    val cid = fetchCid(uri)?.takeIf(String::isNotBlank)
        ?: throw WriterMetadataException(
            "Couldn't find that Bluesky post. Check the at:// URI, or clear the field to publish without it.",
        )
    return draft.copy(bskyPostUri = uri, bskyPostCid = cid)
}

/**
 * Returns [record] with the draft's metadata written over it.
 *
 * Every other key — including ones this client does not model, and the
 * existing `links` value — passes through untouched. Metadata the user
 * cleared is removed rather than written as an empty container, per
 * [DocumentMetadata.applyTo].
 */
internal fun applyDocumentMetadata(record: JsonObject, draft: WriterMetadataDraft): JsonObject {
    val written = JsonMapBridge.mapToJson(DocumentMetadata.applyTo(emptyMap(), draft.toDocumentMetadata()))
    val replacedKeys = DocumentMetadata.OWNED_KEYS - "links"
    return buildJsonObject {
        record.forEach { (key, value) -> if (key !in replacedKeys) put(key, value) }
        written.forEach { (key, value) -> put(key, value) }
        draft.coverImage?.let { put("coverImage", it) }
    }
}
