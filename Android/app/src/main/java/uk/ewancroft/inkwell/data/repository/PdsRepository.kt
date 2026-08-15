package uk.ewancroft.inkwell.data.repository

import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import io.github.kikin81.atproto.runtime.XrpcClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.model.common.AtUri
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UserSessionInfo(
    val handle: String,
    val did: String,
    val pdsUrl: String,
)

@Singleton
class PdsRepository @Inject constructor(
    private val atOAuth: AtOAuth,
    private val sessionStore: OAuthSessionStore,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val publicHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val ktorHttpClient = HttpClient(CIO)

    suspend fun getSession(): UserSessionInfo? {
        val session = sessionStore.load() ?: return null
        val pdsUrl = session.pdsUrl ?: return null
        return UserSessionInfo(
            handle = session.handle ?: session.did.orEmpty(),
            did = session.did.orEmpty(),
            pdsUrl = pdsUrl,
        )
    }

    suspend fun listRecords(
        did: String,
        collection: String,
        limit: Int = 25,
        cursor: String? = null,
        pdsUrl: String? = null,
    ): JsonObject {
        val baseUrl = pdsUrl ?: resolvePdsUrl(did) ?: "https://public.api.bsky.app"
        val urlStr = buildString {
            append("$baseUrl/xrpc/com.atproto.repo.listRecords")
            append("?repo=$did")
            append("&collection=$collection")
            append("&limit=$limit")
            cursor?.let { append("&cursor=$it") }
        }
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(urlStr).get().build()
            val response = publicHttpClient.newCall(request).execute()
            json.decodeFromString(response.body!!.string())
        }
    }

    suspend fun getRecord(uri: String, pdsUrl: String? = null): JsonObject {
        val parsed = requireNotNull(AtUri.parse(uri))
        val baseUrl = pdsUrl ?: resolvePdsUrl(parsed.did) ?: "https://public.api.bsky.app"
        val urlStr = "$baseUrl/xrpc/com.atproto.repo.getRecord?repo=${parsed.did}&collection=${parsed.collection}&rkey=${parsed.recordKey}"
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(urlStr).get().build()
            val response = publicHttpClient.newCall(request).execute()
            json.decodeFromString(response.body!!.string())
        }
    }

    suspend fun createRecord(
        collection: String,
        record: JsonObject,
    ): JsonObject {
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val authClient = atOAuth.createClient()
        return authClient.procedure(
            nsid = "com.atproto.repo.createRecord",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            input = buildJsonObject {
                put("repo", session.did)
                put("collection", collection)
                put("record", record)
            },
            inputSerializer = JsonObject.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
    }

    suspend fun createPublication(
        url: String,
        name: String,
        description: String? = null,
    ): JsonObject {
        val record = buildJsonObject {
            put("\$type", "site.standard.publication")
            put("url", url)
            put("name", name)
            if (description != null) put("description", description)
        }
        return createRecord("site.standard.publication", record)
    }

    suspend fun deleteRecord(collection: String, rkey: String) {
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val authClient = atOAuth.createClient()
        authClient.procedure(
            nsid = "com.atproto.repo.deleteRecord",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            input = buildJsonObject {
                put("repo", session.did)
                put("collection", collection)
                put("rkey", rkey)
            },
            inputSerializer = JsonObject.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
    }

    // ── Graph: Subscriptions (site.standard.graph.subscription) ────────────
    //
    // A follow-style edge: the subscriber is whoever's repo the record lives
    // in (same as app.bsky.graph.follow) — there is no separate subscriber
    // field. Mirrors Inkwell iOS LoginStateManager.createSubscription /
    // fetchSubscriptions / deleteSubscription.

    private companion object {
        const val SUBSCRIPTION_COLLECTION = "site.standard.graph.subscription"
        const val RECOMMEND_COLLECTION = "site.standard.graph.recommend"
        const val COMMENT_COLLECTION = "pub.leaflet.comment"
    }

    data class SubscriptionEntry(val uri: String, val rkey: String, val publicationUri: String)

    /** Creates a `site.standard.graph.subscription` record: subscribes the signed-in user to [publicationUri]. */
    suspend fun createSubscription(publicationUri: String): JsonObject {
        require(AtUri.parse(publicationUri)?.collection == "site.standard.publication") {
            "publicationUri must reference a site.standard.publication record"
        }
        val record = buildJsonObject {
            put("\$type", SUBSCRIPTION_COLLECTION)
            put("publication", publicationUri)
            put("createdAt", Instant.now().toString())
        }
        return createRecord(SUBSCRIPTION_COLLECTION, record)
    }

    /** Lists the signed-in user's own subscriptions (paginated to completion). */
    suspend fun fetchSubscriptions(did: String, pdsUrl: String? = null): List<SubscriptionEntry> =
        listAllRecords(did, SUBSCRIPTION_COLLECTION, pdsUrl).mapNotNull { entry ->
            try {
                val publication = entry.value["publication"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
                SubscriptionEntry(entry.uri, rkey, publication)
            } catch (_: Exception) { null }
        }

    /** Deletes a subscription record by its record key. */
    suspend fun deleteSubscription(rkey: String) = deleteRecord(SUBSCRIPTION_COLLECTION, rkey)

    // ── Graph: Recommends (site.standard.graph.recommend) ──────────────────
    //
    // A lightweight social signal (like/bookmark) pointing at a single
    // document. Mirrors Inkwell iOS LoginStateManager.createRecommend /
    // fetchRecommends / deleteRecommend. Cross-repo counts/discovery use
    // ConstellationClient, not this (local-repo-only) list.

    data class RecommendEntry(val uri: String, val rkey: String, val documentUri: String)

    /** Creates a `site.standard.graph.recommend` record: recommends [documentUri]. */
    suspend fun createRecommend(documentUri: String): JsonObject {
        require(AtUri.parse(documentUri)?.collection == "site.standard.document") {
            "documentUri must reference a site.standard.document record"
        }
        val record = buildJsonObject {
            put("\$type", RECOMMEND_COLLECTION)
            put("document", documentUri)
            put("createdAt", Instant.now().toString())
        }
        return createRecord(RECOMMEND_COLLECTION, record)
    }

    /** Lists the signed-in user's own recommends (local repo only; paginated to completion). */
    suspend fun fetchRecommends(did: String, pdsUrl: String? = null): List<RecommendEntry> =
        listAllRecords(did, RECOMMEND_COLLECTION, pdsUrl).mapNotNull { entry ->
            try {
                val document = entry.value["document"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
                RecommendEntry(entry.uri, rkey, document)
            } catch (_: Exception) { null }
        }

    /** Deletes a recommend record by its record key. */
    suspend fun deleteRecommend(rkey: String) = deleteRecord(RECOMMEND_COLLECTION, rkey)

    // ── Comments (pub.leaflet.comment) ─────────────────────────────────────

    data class CommentEntry(val uri: String, val rkey: String, val comment: uk.ewancroft.inkwell.data.model.graph.LeafletComment)

    /** Creates a `pub.leaflet.comment` record. */
    suspend fun createComment(
        subject: String,
        plaintext: String,
        replyTo: String? = null,
        onPage: String? = null
    ): JsonObject {
        val record = buildJsonObject {
            put("\$type", COMMENT_COLLECTION)
            put("subject", subject)
            put("plaintext", plaintext)
            if (replyTo != null) {
                put("reply", buildJsonObject { put("parent", replyTo) })
            }
            if (onPage != null) put("onPage", onPage)
        }
        return createRecord(COMMENT_COLLECTION, record)
    }

    /** Lists the signed-in user's own comments (paginated to completion). */
    suspend fun fetchComments(did: String, pdsUrl: String? = null): List<CommentEntry> =
        listAllRecords(did, COMMENT_COLLECTION, pdsUrl).mapNotNull { entry ->
            try {
                val commentJson = entry.value
                val comment = json.decodeFromJsonElement(uk.ewancroft.inkwell.data.model.graph.LeafletComment.serializer(), commentJson)
                val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
                CommentEntry(entry.uri, rkey, comment)
            } catch (_: Exception) { null }
        }

    /** Deletes a comment record by its record key. */
    suspend fun deleteComment(rkey: String) = deleteRecord(COMMENT_COLLECTION, rkey)

    // ── Pagination helper ────────────────────────────────────────────────

    private data class RawRecordEntry(val uri: String, val value: JsonObject)

    /**
     * Paginates `com.atproto.repo.listRecords` to completion. Capped at
     * [maxRecords] so a misbehaving PDS returning an endless cursor can't
     * hang the caller forever.
     */
    private suspend fun listAllRecords(
        did: String,
        collection: String,
        pdsUrl: String? = null,
        maxRecords: Int = 500,
    ): List<RawRecordEntry> {
        val all = mutableListOf<RawRecordEntry>()
        var cursor: String? = null
        do {
            val response = listRecords(did = did, collection = collection, cursor = cursor, pdsUrl = pdsUrl)
            val records = response["records"]?.jsonArray.orEmpty()
            if (records.isEmpty()) break
            for (r in records) {
                val obj = r.jsonObject
                val uri = obj["uri"]?.jsonPrimitive?.content ?: continue
                val value = obj["value"]?.jsonObject ?: continue
                all.add(RawRecordEntry(uri, value))
            }
            cursor = response["cursor"]?.jsonPrimitive?.contentOrNull
        } while (cursor != null && all.size < maxRecords)
        return all.take(maxRecords)
    }

    suspend fun resolveHandle(handle: String): String {
        val urlStr = "https://public.api.bsky.app/xrpc/com.atproto.identity.resolveHandle?handle=$handle"
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(urlStr).get().build()
            val response = publicHttpClient.newCall(request).execute()
            val body: JsonObject = json.decodeFromString(response.body!!.string())
            body["did"]!!.jsonPrimitive.content
        }
    }

    suspend fun getProfile(did: String): BlueskyProfile {
        val urlStr = "https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=$did"
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(urlStr).get().build()
            val response = publicHttpClient.newCall(request).execute()
            json.decodeFromString(response.body!!.string())
        }
    }

    private suspend fun resolvePdsUrl(did: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val urlStr = "https://plc.directory/$did"
                val request = Request.Builder().url(urlStr).get().build()
                val response = publicHttpClient.newCall(request).execute()
                val body = json.parseToJsonElement(response.body!!.string()).jsonObject
                val services = body["service"]?.jsonArray
                    ?: body["services"]?.jsonArray
                services?.firstOrNull { service ->
                    val type = service.jsonObject["type"]?.jsonPrimitive?.content
                    type == "AtprotoPersonalDataServer" || type == "PersonalDataServer"
                }?.jsonObject?.get("serviceEndpoint")?.jsonPrimitive?.content
                    ?: body["pdsUrl"]?.jsonPrimitive?.content
            } catch (_: Exception) { null }
        }
    }
}
