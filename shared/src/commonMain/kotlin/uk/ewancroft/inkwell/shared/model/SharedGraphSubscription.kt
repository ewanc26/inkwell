package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a `site.standard.graph.subscription` record.
 *
 * Mirrors Android `GraphSubscription` and iOS `Graph.SubscriptionRecord`.
 */
data class SharedGraphSubscription(
    val type: String = "site.standard.graph.subscription",
    val publication: String,
    val createdAt: String? = null
)
