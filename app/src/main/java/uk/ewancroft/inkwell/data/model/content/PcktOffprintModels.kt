/**
 * Pckt and Offprint content models — secondary block-array formats
 * supported by Inkwell alongside Leaflet.
 *
 * Both formats use the same inline facet shape as Leaflet, so we reuse
 * LeafletFacet/ByteSlice/FacetFeature from the Leaflet model package.
 */
package uk.ewancroft.inkwell.data.model.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.ewancroft.inkwell.data.model.common.BlobRef

// ── Pckt (blog.pckt.content) ──────────────────────────────────────────────

@Serializable
data class PcktContent(
    @SerialName("\$type") val type: String = "blog.pckt.content",
    val items: List<PcktBlock>? = null
)

@Serializable
data class PcktBlock(
    @SerialName("\$type") val type: String,
    val plaintext: String? = null,
    val level: Int? = null,
    val facets: List<LeafletFacet>? = null,
    val language: String? = null,
    val content: List<PcktBlock>? = null,
    val listContent: List<PcktListItem>? = null,
    val start: Int? = null,
    val attrs: PcktAttrs? = null,
    val children: List<PcktBlock>? = null,
    val checked: Boolean? = null,
)

@Serializable
data class PcktListItem(
    @SerialName("\$type") val type: String,
    val content: List<PcktBlock>? = null,
    val checked: Boolean? = null,
)

@Serializable
data class PcktAttrs(
    val src: String? = null,
    val alt: String? = null,
)

// ── Offprint (app.offprint.content) ───────────────────────────────────────

@Serializable
data class OffprintContent(
    @SerialName("\$type") val type: String = "app.offprint.content",
    val items: List<OffprintBlock>? = null
)

@Serializable
data class OffprintBlock(
    @SerialName("\$type") val type: String,
    val plaintext: String? = null,
    val level: Int? = null,
    val facets: List<LeafletFacet>? = null,
    val language: String? = null,
    val content: List<OffprintBlock>? = null,
    val children: List<OffprintBlock>? = null,
    val start: Int? = null,
    val image: BlobRef? = null,
    val alt: String? = null,
    val checked: Boolean? = null,
)
