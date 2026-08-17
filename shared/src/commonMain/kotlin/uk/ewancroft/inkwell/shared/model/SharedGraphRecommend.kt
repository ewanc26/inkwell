package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a `site.standard.graph.recommend` record.
 *
 * Mirrors Android `GraphRecommend` and iOS `Graph.RecommendRecord`.
 */
data class SharedGraphRecommend(
    val type: String = "site.standard.graph.recommend",
    val document: String,
    val createdAt: String? = null
)
