/**
 * Shared primitives and generic AT Protocol API types used across all lexicons.
 *
 * These are the Kotlin equivalents of Inkwell iOS's StrongRef, BlobRef, AtUri,
 * and generic paginated response wrappers. Every record type in the app either
 * uses or contains one of these shapes.
 */
package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

// ── Primitives ───────────────────────────────────────────────────────────

/**
 * Reference to an uploaded blob (image, file).
 *
 * The real AT Protocol wire shape nests the CID under `ref.$link`:
 * `{"$type":"blob","ref":{"$link":"..."},"mimeType":"...","size":...}` — see
 * `com.atproto.repo.uploadBlob`'s `BlobRef` in the atproto spec, and the pinned
 * `site.standard.document`/`site.standard.publication` Lexicons' `blob` fields
 * in `lexicons/standard-site/schemas/`. [BlobRefSerializer] maps that wire shape
 * to/from this flat Kotlin type so every other call site keeps reading/writing
 * a plain `link` property.
 */
@Serializable(with = BlobRefSerializer::class)
data class BlobRef(
    val link: String,
    val size: Int = 0,
    val type: String = "blob",
    val mimeType: String? = null
)

/**
 * Hand-written rather than `@Serializable` + `@SerialName("ref")`-wrapping, so
 * [BlobRef.link] stays a flat top-level Kotlin property instead of forcing
 * every reader/writer of it to add a `.ref` hop. See issue #65 — decoding a
 * real `site.standard.publication.icon` or `site.standard.document.coverImage`
 * fixture into the previous flat-JSON-shaped `BlobRef` threw a missing-field
 * error, because no publisher actually emits `$link` at the top level.
 */
object BlobRefSerializer : KSerializer<BlobRef> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BlobRef", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BlobRef) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("BlobRef can only be serialized to JSON")
        val json = buildJsonObject {
            put("\$type", value.type)
            putJsonObject("ref") { put("\$link", value.link) }
            value.mimeType?.let { put("mimeType", it) }
            put("size", value.size)
        }
        jsonEncoder.encodeJsonElement(json)
    }

    override fun deserialize(decoder: Decoder): BlobRef {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("BlobRef can only be deserialized from JSON")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val link = obj["ref"]?.jsonObject?.get("\$link")?.jsonPrimitive?.contentOrNull
            // Tolerate the old (incorrect) flat shape too, in case anything
            // still round-trips it from a previously-cached record.
            ?: obj["\$link"]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("BlobRef is missing ref.\$link")
        return BlobRef(
            link = link,
            size = obj["size"]?.jsonPrimitive?.int ?: 0,
            type = obj["\$type"]?.jsonPrimitive?.contentOrNull ?: "blob",
            mimeType = obj["mimeType"]?.jsonPrimitive?.contentOrNull
        )
    }
}

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
 * Parsed AT-URI — re-exported from the shared KMP core so existing
 * @Serializable usages in this module resolve to the same type.
 */
typealias AtUri = uk.ewancroft.inkwell.shared.AtUri

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
