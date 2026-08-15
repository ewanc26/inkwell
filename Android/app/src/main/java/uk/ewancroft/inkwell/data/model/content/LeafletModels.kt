/**
 * Leaflet block content model — Inkwell's primary rich-text format.
 *
 * Leaflet is a block-based content format built on AT Protocol records:
 * paragraphs, headers, images, code blocks, embeds, checklists, and more.
 * Each document can have multiple pages, each page contains an ordered list
 * of blocks, and blocks can nest (lists within lists, embeds within text).
 *
 * Mirrors the Inkwell iOS LeafletContent models and the upstream Leaflet spec.
 */
package uk.ewancroft.inkwell.data.model.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.ewancroft.inkwell.data.model.common.BlobRef
import uk.ewancroft.inkwell.data.model.common.StrongRef

// ── Top-Level Content Container ──────────────────────────────────────────

/** Root wrapper: a Leaflet document is a list of pages. */
@Serializable
data class LeafletContent(
    @SerialName("\$type") val type: String = "pub.leaflet.content",
    val pages: List<LeafletPage>? = null
)

// ── Pages ────────────────────────────────────────────────────────────────

/**
 * A single page in a multi-page Leaflet document.
 * Each page holds an ordered list of block containers.
 */
@Serializable
data class LeafletPage(
    @SerialName("\$type") val type: String = "pub.leaflet.pages.linearDocument",
    val id: String,
    val blocks: List<BlockContainer>? = null
)

/** Wraps a block with optional alignment metadata. */
@Serializable
data class BlockContainer(
    @SerialName("\$type") val type: String = "pub.leaflet.pages.linearDocument#block",
    val block: LeafletBlock,
    val alignment: String? = null
)

// ── Block Types ──────────────────────────────────────────────────────────

/**
 * A single content block. The $type discriminator determines which fields
 * are populated — each block type uses a subset of the optional fields.
 *
 * Block types include: text, header, blockquote, code, math, image,
 * ordered/unordered list, embed (post/recommend/website), button, iframe,
 * page reference, divider, and checklist items.
 */
@Serializable
data class LeafletBlock(
    @SerialName("\$type") val type: String,
    // text/header/blockquote
    val plaintext: String? = null,
    val level: Int? = null,
    val facets: List<LeafletFacet>? = null,
    // code
    val language: String? = null,
    // math
    val tex: String? = null,
    // image
    val image: BlobRef? = null,
    val alt: String? = null,
    // lists
    val children: List<ListItem>? = null,
    val startIndex: Int? = null,
    // embeds
    val subject: StrongRef? = null,
    val standardSitePostSubject: String? = null,
    val standardSitePostCID: String? = null,
    val size: String? = null,
    val showPublicationTheme: Boolean? = null,
    val clientHost: String? = null,
    // website embed
    val url: String? = null,
    val websiteTitle: String? = null,
    val websiteDescription: String? = null,
    // button
    val text: String? = null,
    // iframe
    val height: Double? = null,
    val aspectRatio: String? = null,
    // page reference
    val pageIndex: Int? = null,
    val pageDocument: String? = null
)

// ── List Items & Facets ──────────────────────────────────────────────────

/** An item in an ordered/unordered/checklist. Can nest further items. */
@Serializable
data class ListItem(
    @SerialName("\$type") val type: String,
    val content: LeafletBlock? = null,
    val checked: Boolean? = null,
    val children: List<ListItem>? = null
)

/**
 * A rich-text facet (link, mention) applied to a byte range within
 * a text block. Mirrors app.bsky.richtext.facet's byte-slice indexing.
 */
@Serializable
data class LeafletFacet(
    val index: ByteSlice,
    val features: List<FacetFeature>
)

/** Inclusive byte range within a text block's UTF-8 representation. */
@Serializable
data class ByteSlice(
    val byteStart: Int,
    val byteEnd: Int
)

/** A single facet feature — link URI, DID mention, or custom type. */
@Serializable
data class FacetFeature(
    @SerialName("\$type") val type: String,
    val uri: String? = null,
    val did: String? = null
)
