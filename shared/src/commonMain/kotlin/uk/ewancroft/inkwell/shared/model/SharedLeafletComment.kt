package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a `pub.leaflet.comment` record.
 *
 * Mirrors Android `LeafletComment` and iOS `PubLeafletComment`.
 */
data class SharedLeafletComment(
    val type: String = "pub.leaflet.comment",
    val subject: String,
    val createdAt: String? = null,
    val plaintext: String,
    val facets: List<LeafletFacet>? = null,
    val reply: ReplyRef? = null,
    val onPage: String? = null
) {
    data class ReplyRef(
        val parent: String
    )
}
