/**
 * Shared primitives and generic AT Protocol API types used across all lexicons.
 *
 * These are the Kotlin equivalents of Inkwell iOS's StrongRef, BlobRef, AtUri,
 * and generic paginated response wrappers. Every record type in the app either
 * uses or contains one of these shapes.
 */
package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Primitives ───────────────────────────────────────────────────────────

/** Reference to an uploaded blob (image, file) — CBOR-encoded $link in AT Proto. */
@Serializable
data class BlobRef(
    @SerialName("\$link") val link: String,
    val size: Int = 0,
    @SerialName("\$type") val type: String = "blob",
    val mimeType: String? = null
)

/**
 * Strong reference to any AT Protocol record: URI + optional content hash.
 * Used wherever one record needs to cite another (e.g. a comment referencing
 * its parent document).
 */
@Serializable
data class StrongRef(
    val uri: String,
    val cid: String? = null
)

/**
 * Polymorphic content container. The $type discriminator determines
 * whether this holds Leaflet blocks, Markdown text, or another format.
 */
@Serializable
data class ContentUnion(
    @SerialName("\$type") val type: String,
    val pages: List<uk.ewancroft.inkwell.data.model.content.LeafletPage>? = null
)

// ── AT Protocol URI & Pagination Types ───────────────────────────────────

/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 */
@Serializable
data class AtUri(val did: String, val collection: String, val recordKey: String) {
    companion object {
        /** Parses at:// URIs. Returns null for malformed input. */
        fun parse(uri: String): AtUri? {
            val regex = Regex("""^at://([^/]+)/([^/]+)/(.+)$""")
            val match = regex.find(uri) ?: return null
            return AtUri(match.groupValues[1], match.groupValues[2], match.groupValues[3])
        }
    }
    /** Reassembles the canonical AT-URI string. */
    val uri: String get() = "at://$did/$collection/$recordKey"
}

/** Generic wrapper for a single record returned from the PDS. */
@Serializable
data class RecordEntry<T>(
    val uri: String,
    val cid: String? = null,
    val value: T
)

/** Generic paginated response from com.atproto.repo.listRecords. */
@Serializable
data class ListRecordsResponse<T>(
    val records: List<RecordEntry<T>>,
    val cursor: String? = null
)
