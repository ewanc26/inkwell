package uk.ewancroft.inkwell.shared.feed

import kotlinx.serialization.Serializable

/**
 * A feed item persisted in the local cache.
 *
 * This is the serialisation boundary between the shared KMP layer and
 * the platform-specific storage backends.  Both iOS and Android read
 * and write lists of these objects as JSON.
 *
 * Fields are intentionally denormalised — publication name / URL and
 * author display-name / avatar are snapshot copies taken at cache time
 * so the feed can render immediately without a secondary profile or
 * publication lookup.
 */
@Serializable
data class CachedFeedItem(
    /** AT URI of the `site.standard.document` record. */
    val uri: String,
    /** DID of the publication / author that owns the document. */
    val authorDID: String,
    /** AT URI of the `site.standard.publication` this doc belongs to. */
    val site: String,
    val title: String,
    /** ISO-8601 timestamp of when the document was published. */
    val publishedAt: String,
    val path: String? = null,
    val description: String? = null,
    val textContent: String? = null,
    val coverImageUrl: String? = null,
    /** AT URI of the publication record. */
    val publicationUri: String? = null,
    /** Human-readable publication name (snapshot). */
    val publicationName: String? = null,
    /** Canonical website URL of the publication (snapshot). */
    val publicationUrl: String? = null,
    /** Snapshot of the author's display name. */
    val authorDisplayName: String? = null,
    /** Snapshot of the author's avatar URL. */
    val authorAvatar: String? = null,
    /** Epoch-millis timestamp of when this item was written to cache. */
    val cachedAt: Long = 0
)
