/**
 * Pckt and Offprint content models — secondary block-array formats
 * supported by Inkwell alongside Leaflet.
 *
 * Both formats use the same inline facet shape as Leaflet, so we reuse
 * LeafletFacet/ByteSlice/FacetFeature from the Leaflet model package.
 *
 * Wire models match the authoritative pckt.blog and offprint.app lexicons
 * published at at://did:plc:revjuqmkvrw6fnkxppqtszpv/com.atproto.lexicon.schema
 * and at://did:plc:pgjkomf37an4czloay5zeth6/com.atproto.lexicon.schema.
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
    val textAlign: String? = null,
    val content: List<PcktBlock>? = null,
    val listContent: List<PcktListItem>? = null,
    val tableRows: List<PcktTableRow>? = null,
    val start: Int? = null,
    val attrs: PcktAttrs? = null,
    val children: List<PcktBlock>? = null,
    val checked: Boolean? = null,
    val did: String? = null,
    val handle: String? = null,
    val ref: String? = null,
    val url: String? = null,
    val height: Int? = null,
    val websiteTitle: String? = null,
    val websiteDescription: String? = null,
    val previewImage: String? = null,
    val postRef: String? = null,
    val noteRef: String? = null,
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
    val align: String? = null,
    val title: String? = null,
    val aspectRatio: PcktAspectRatio? = null,
)

@Serializable
data class PcktAspectRatio(
    val width: Int,
    val height: Int,
)

@Serializable
data class PcktTableRow(
    val content: List<PcktTableCell>,
)

@Serializable
data class PcktTableCell(
    val content: List<PcktBlock>,
    val colspan: Int? = null,
    val rowspan: Int? = null,
)

@Serializable
data class PcktTableHeader(
    val content: List<PcktBlock>,
    val colspan: Int? = null,
    val rowspan: Int? = null,
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
    val textAlign: String? = null,
    val code: String? = null,
    val language: String? = null,
    val showLineNumbers: Boolean? = null,
    val content: List<OffprintBlock>? = null,
    val children: List<OffprintListItem>? = null,
    val start: Int? = null,
    val image: BlobRef? = null,
    val alt: String? = null,
    val width: String? = null,
    val caption: String? = null,
    val alignment: String? = null,
    val aspectRatio: OffprintAspectRatio? = null,
    val captionFacets: List<LeafletFacet>? = null,
    val images: List<OffprintGridImage>? = null,
    val gridRows: Int? = null,
    val href: String? = null,
    val title: String? = null,
    val preview: BlobRef? = null,
    val siteName: String? = null,
    val description: String? = null,
    val embedUrl: String? = null,
    val embedWidth: Int? = null,
    val embedHeight: Int? = null,
    val post: OffprintStrongRef? = null,
)

@Serializable
data class OffprintListItem(
    val content: OffprintBlock? = null,
    val checked: Boolean? = null,
    val children: List<OffprintListItem>? = null,
)

@Serializable
data class OffprintAspectRatio(
    val width: Int,
    val height: Int,
)

@Serializable
data class OffprintGridImage(
    val alt: String? = null,
    val blob: BlobRef? = null,
    val aspectRatio: OffprintAspectRatio? = null,
)

@Serializable
data class OffprintStrongRef(
    val uri: String,
    val cid: String,
)
