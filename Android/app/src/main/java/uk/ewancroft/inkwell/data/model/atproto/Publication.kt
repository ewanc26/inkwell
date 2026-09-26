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
import kotlinx.serialization.json.JsonElement
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
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
    @SerialName("\$type") val type: String = CollectionNsids.PUBLICATION,
    val url: String,
    val name: String,
    val description: String? = null,
    val icon: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val basicTheme: BasicTheme? = null,
    val labels: SelfLabels? = null,
    val preferences: PublicationPreferences? = null
)

@Serializable
data class SelfLabels(val values: List<SelfLabel> = emptyList())

@Serializable
data class SelfLabel(
    @SerialName("val") val value: String,
    /** Labeler DID/service when a record response preserves its source. */
    @SerialName("src") val source: String? = null,
)

// ── standard.site: document ───────────────────────────────────────────────

/**
 * A single published post or page. Can carry content in one of several
 * formats (Leaflet blocks, Markdown text, etc.) and optionally links
 * back to a Bluesky post for cross-protocol federation.
 */
@Serializable
data class DocumentRecord(
    @SerialName("\$type") val type: String = CollectionNsids.DOCUMENT,
    val site: String,
    val title: String,
    val publishedAt: String,
    val updatedAt: String? = null,
    val path: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val content: uk.ewancroft.inkwell.data.model.common.ContentUnion? = null,
    val textContent: String? = null,
    val coverImage: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val labels: SelfLabels? = null,
    val preferences: DocumentPreferences? = null,
    val bskyPostRef: StrongRef? = null,
    /**
     * Additional contributors to this document, beyond the record's author.
     * Mirrors iOS's `SiteStandardLexicon.DocumentRecord.Contributor` — see
     * issue #65, which named this field's Android absence as the motivating
     * example of undetected cross-platform model drift.
     */
    val contributors: List<DocumentContributor>? = null,
    /**
     * Describes relationships between this document and external resources.
     * An open union with no concrete Lexicon-declared shape (same rationale
     * as [content]), so it's kept as a raw [JsonElement] rather than a typed
     * model, mirroring iOS's `UnknownType` field of the same name.
     */
    val links: JsonElement? = null
)

/** A participant on a document beyond the record's author. */
@Serializable
data class DocumentContributor(
    @SerialName("\$type") val type: String = "site.standard.document#contributor",
    val did: String,
    val role: String? = null,
    val displayName: String? = null
)
