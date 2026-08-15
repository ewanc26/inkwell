/**
 * Graph subscription and recommendation record shapes.
 *
 * These are the edges in the standard.site social graph: subscribing to a
 * publication (follow) and recommending a document (like/bookmark). Both
 * are stored as AT Protocol records in the user's repository and mirrored
 * in the Constellation index for cross-repo discovery.
 */
package uk.ewancroft.inkwell.data.model.graph

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── standard.site: subscription (follow) ─────────────────────────────────

/** A follow/subscription edge: user subscribes to a publication. */
@Serializable
data class GraphSubscription(
    @SerialName("\$type") val type: String = "site.standard.graph.subscription",
    val publication: String,
    val createdAt: String? = null
)

// ── standard.site: recommend (like) ──────────────────────────────────────

/** A recommendation edge: user recommends (likes/bookmarks) a document. */
@Serializable
data class GraphRecommend(
    @SerialName("\$type") val type: String = "site.standard.graph.recommend",
    val document: String,
    val createdAt: String? = null
)

// ── Leaflet: comments ─────────────────────────────────────────────────────

/** A comment on a document, stored as `pub.leaflet.comment`. */
@Serializable
data class LeafletComment(
    @SerialName("\$type") val type: String = "pub.leaflet.comment",
    val subject: String,
    val plaintext: String,
    val reply: ReplyRef? = null,
    val onPage: String? = null,
    val createdAt: String? = null
) {
    @Serializable
    data class ReplyRef(
        val parent: String
    )
}

/** A hydrated comment entry from the network. */
data class CommentEntry(
    val uri: String,
    val recordKey: String,
    val record: LeafletComment,
    val authorDid: String? = null,
    val authorDisplayName: String? = null
)
