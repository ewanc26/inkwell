package uk.ewancroft.inkwell.data.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import java.time.Instant

// ── Graph: Subscriptions (site.standard.graph.subscription) ────────────
//
// A follow-style edge: the subscriber is whoever's repo the record lives
// in (same as app.bsky.graph.follow) — there is no separate subscriber
// field. Mirrors Inkwell iOS LoginStateManager.createSubscription /
// fetchSubscriptions / deleteSubscription.

data class SubscriptionEntry(val uri: String, val rkey: String, val publicationUri: String)

/** Creates a `site.standard.graph.subscription` record: subscribes the signed-in user to [publicationUri]. */
suspend fun PdsRepository.createSubscription(publicationUri: String): JsonObject {
    require(AtUri.parse(publicationUri)?.collection == CollectionNsids.PUBLICATION) {
        "publicationUri must reference a site.standard.publication record"
    }
    val record = buildJsonObject {
        put("\$type", CollectionNsids.GRAPH_SUBSCRIPTION)
        put("publication", publicationUri)
        put("createdAt", Instant.now().toString())
    }
    return createRecord(CollectionNsids.GRAPH_SUBSCRIPTION, record)
}

/** Lists the signed-in user's own subscriptions (paginated to completion). */
suspend fun PdsRepository.fetchSubscriptions(did: String, pdsUrl: String? = null): List<SubscriptionEntry> =
    listAllRecords(did, CollectionNsids.GRAPH_SUBSCRIPTION, pdsUrl).mapNotNull { entry ->
        try {
            val publication = entry.value["publication"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
            SubscriptionEntry(entry.uri, rkey, publication)
        } catch (_: Exception) { null }
    }

/** Deletes a subscription record by its record key. */
suspend fun PdsRepository.deleteSubscription(rkey: String) = deleteRecord(CollectionNsids.GRAPH_SUBSCRIPTION, rkey)

// ── Graph: Recommends (site.standard.graph.recommend) ──────────────────
//
// A lightweight social signal (like/bookmark) pointing at a single
// document. Mirrors Inkwell iOS LoginStateManager.createRecommend /
// fetchRecommends / deleteRecommend. Cross-repo counts/discovery use
// ConstellationClient, not this (local-repo-only) list.

data class RecommendEntry(val uri: String, val rkey: String, val documentUri: String)

/** Creates a `site.standard.graph.recommend` record: recommends [documentUri]. */
suspend fun PdsRepository.createRecommend(documentUri: String): JsonObject {
    require(AtUri.parse(documentUri)?.collection == CollectionNsids.DOCUMENT) {
        "documentUri must reference a site.standard.document record"
    }
    val record = buildJsonObject {
        put("\$type", CollectionNsids.GRAPH_RECOMMEND)
        put("document", documentUri)
        put("createdAt", Instant.now().toString())
    }
    return createRecord(CollectionNsids.GRAPH_RECOMMEND, record)
}

/** Lists the signed-in user's own recommends (local repo only; paginated to completion). */
suspend fun PdsRepository.fetchRecommends(did: String, pdsUrl: String? = null): List<RecommendEntry> =
    listAllRecords(did, CollectionNsids.GRAPH_RECOMMEND, pdsUrl).mapNotNull { entry ->
        try {
            val document = entry.value["document"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
            RecommendEntry(entry.uri, rkey, document)
        } catch (_: Exception) { null }
    }

/** Deletes a recommend record by its record key. */
suspend fun PdsRepository.deleteRecommend(rkey: String) = deleteRecord(CollectionNsids.GRAPH_RECOMMEND, rkey)
