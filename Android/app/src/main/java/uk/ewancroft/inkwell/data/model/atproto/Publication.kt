/**
 * AT Protocol record shapes for the standard.site publishing lexicon.
 *
 * These map directly to the NSManagedObject subclasses in Inkwell iOS:
 * SitePublication and SiteDocument. The @SerialName annotations match the
 * lexical type identifiers that the AT Protocol firehose and PDS use for
 * record routing.
 */
package uk.ewancroft.inkwell.data.model.atproto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import uk.ewancroft.inkwell.data.model.common.BlobRef
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletPage
import uk.ewancroft.inkwell.data.model.content.LeafletContent

// ── standard.site: publication ────────────────────────────────────────────

/**
 * A blog or publishing entity. Every document lives under a publication,
 * which owns its theme, icon, discovery preferences.
 */
@Serializable
data class PublicationRecord(
    @SerialName("\$type") val type: String = "site.standard.publication",
    val url: String,
    val name: String,
    val description: String? = null,
    val icon: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val basicTheme: BasicTheme? = null,
    val preferences: PublicationPreferences? = null
)

// ── standard.site: document ───────────────────────────────────────────────

/**
 * A single published post or page. Can carry content in one of several
 * formats (Leaflet blocks, Markdown text, etc.) and optionally links
 * back to a Bluesky post for cross-protocol federation.
 */
@Serializable
data class DocumentRecord(
    @SerialName("\$type") val type: String = "site.standard.document",
    val site: String,
    val title: String,
    val publishedAt: String,
    val path: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val content: uk.ewancroft.inkwell.data.model.common.ContentUnion? = null,
    val textContent: String? = null,
    val coverImage: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val preferences: DocumentPreferences? = null,
    val bskyPostRef: StrongRef? = null
)
