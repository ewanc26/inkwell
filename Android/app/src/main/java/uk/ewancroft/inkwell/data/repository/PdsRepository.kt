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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.content.ContentFormatDetector
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.util.HandleUtils
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.data.model.content.LeafletPollDefinition
import uk.ewancroft.inkwell.data.model.content.LeafletPollVote
import uk.ewancroft.inkwell.shared.policy.RecordListPolicy
import java.net.URLEncoder
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

    /** URL-encodes a query param value — DIDs/AT-URIs can contain `:`, `/`,
     *  `.`, and other characters that must not be interpolated raw into a
     *  query string. */
    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Executes a GET and returns the raw body, throwing on non-2xx statuses
     *  or a missing body instead of decoding an error payload as success. */
    private suspend fun executeGet(urlStr: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(urlStr).get().build()
        publicHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("PDS request failed: HTTP ${response.code}")
            }
            response.body?.string() ?: throw java.io.IOException("PDS request returned no body")
        }
    }

    suspend fun listRecords(
        did: String,
        collection: String,
        limit: Int = 25,
        cursor: String? = null,
        pdsUrl: String? = null,
    ): JsonObject {
        val baseUrl = pdsUrl ?: resolvePdsUrl(did) ?: XrpcEndpoints.PUBLIC_BSKY_API
        val urlStr = buildString {
            append("$baseUrl${XrpcEndpoints.REPO_LIST_RECORDS}")
            append("?repo=").append(enc(did))
            append("&collection=").append(enc(collection))
            append("&limit=$limit")
            cursor?.let { append("&cursor=").append(enc(it)) }
        }
        return json.decodeFromString(executeGet(urlStr))
    }

    suspend fun getRecord(uri: String, pdsUrl: String? = null): JsonObject {
        val parsed = requireNotNull(AtUri.parse(uri))
        val baseUrl = pdsUrl ?: resolvePdsUrl(parsed.did) ?: XrpcEndpoints.PUBLIC_BSKY_API
        val urlStr = buildString {
            append("$baseUrl${XrpcEndpoints.REPO_GET_RECORD}")
            append("?repo=").append(enc(parsed.did))
            append("&collection=").append(enc(parsed.collection))
            append("&rkey=").append(enc(parsed.recordKey))
        }
        return json.decodeFromString(executeGet(urlStr))
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

    suspend fun updateRecord(
        uri: String,
        record: JsonObject,
        revision: String,
    ): JsonObject {
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val parsed = AtUri.parse(uri) ?: throw IllegalArgumentException("Invalid AT-URI: $uri")
        val authClient = atOAuth.createClient()
        return authClient.procedure(
            nsid = "com.atproto.repo.putRecord",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            input = buildJsonObject {
                put("repo", session.did)
                put("collection", parsed.collection)
                put("rkey", parsed.recordKey)
                put("record", record)
                put("validate", true)
                put("swapCommit", revision)
            },
            inputSerializer = JsonObject.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
    }

    suspend fun uploadBlob(bytes: ByteArray, mimeType: String): JsonObject {
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val authClient = atOAuth.createClient()

        val boundary = "inkwell-upload-${System.currentTimeMillis()}"
        val contentType = io.ktor.http.ContentType.parse("multipart/form-data; boundary=$boundary")

        val body = buildString {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"upload\"; filename=\"blob\"\r\n")
            append("Content-Type: ").append(mimeType).append("\r\n\r\n")
        }.toByteArray(Charsets.UTF_8) + bytes + "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)

        return authClient.procedure(
            nsid = "com.atproto.repo.uploadBlob",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            body,
            contentType,
            JsonObject.serializer(),
        )
    }

    suspend fun createPublication(
        url: String,
        name: String,
        description: String? = null,
    ): JsonObject {
        val record = buildJsonObject {
            put("\$type", CollectionNsids.PUBLICATION)
            put("url", url)
            put("name", name)
            if (description != null) put("description", description)
        }
        return createRecord(CollectionNsids.PUBLICATION, record)
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
        const val SUBSCRIPTION_COLLECTION = CollectionNsids.GRAPH_SUBSCRIPTION
        const val RECOMMEND_COLLECTION = CollectionNsids.GRAPH_RECOMMEND
        const val COMMENT_COLLECTION = CollectionNsids.LEAFLET_COMMENT
    }

    data class SubscriptionEntry(val uri: String, val rkey: String, val publicationUri: String)

    /** Creates a `site.standard.graph.subscription` record: subscribes the signed-in user to [publicationUri]. */
    suspend fun createSubscription(publicationUri: String): JsonObject {
        require(AtUri.parse(publicationUri)?.collection == CollectionNsids.PUBLICATION) {
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
        require(AtUri.parse(documentUri)?.collection == CollectionNsids.DOCUMENT) {
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

    suspend fun fetchDocuments(did: String, pdsUrl: String? = null): List<JsonObject> =
        fetchDocumentEntries(did, pdsUrl).map { it.value }

    data class DocumentRecordEntry(val uri: String, val value: JsonObject)

    suspend fun fetchDocumentEntries(did: String, pdsUrl: String? = null): List<DocumentRecordEntry> =
        listAllRecords(did, CollectionNsids.DOCUMENT, pdsUrl).map { DocumentRecordEntry(it.uri, it.value) }

    // ── Pagination helper ────────────────────────────────────────────────

    internal data class RawRecordEntry(val uri: String, val value: JsonObject)

    /**
     * Paginates `com.atproto.repo.listRecords` to completion. Capped at
     * [maxRecords] so a misbehaving PDS returning an endless cursor can't
     * hang the caller forever.
     */
    private suspend fun listAllRecords(
        did: String,
        collection: String,
        pdsUrl: String? = null,
        maxRecords: Int = RecordListPolicy.MAX_RECORDS,
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
            val nextCursor = response["cursor"]?.jsonPrimitive?.contentOrNull
            // A PDS echoing the same cursor (no new page) would otherwise loop
            // until maxRecords; treat it as the end of the list.
            if (nextCursor == null || nextCursor == cursor) break
            cursor = nextCursor
        } while (all.size < maxRecords)
        return all.take(maxRecords)
    }

    suspend fun resolveHandle(handle: String): String {
        val normalized = HandleUtils.normalize(handle)
        val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.IDENTITY_RESOLVE_HANDLE}?handle=${enc(normalized)}"
        val body: JsonObject = json.decodeFromString(executeGet(urlStr))
        return body["did"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("resolveHandle returned no did")
    }

    suspend fun getProfile(did: String): BlueskyProfile {
        val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.ACTOR_GET_PROFILE}?actor=${enc(did)}"
        return json.decodeFromString(executeGet(urlStr))
    }

    suspend fun downloadBlob(cid: String, fromDID: String): ByteArray = withContext(Dispatchers.IO) {
        val pdsUrl = resolvePdsUrl(fromDID) ?: XrpcEndpoints.PUBLIC_BSKY_API
        val urlStr = "$pdsUrl${XrpcEndpoints.SYNC_GET_BLOB}?cid=${enc(cid)}&did=${enc(fromDID)}"
        val request = Request.Builder().url(urlStr).get().build()
        publicHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Blob download failed: HTTP ${response.code}")
            }
            response.body?.bytes() ?: throw java.io.IOException("Blob response had no body")
        }
    }

    suspend fun getPollDefinition(did: String, rkey: String): LeafletPollDefinition {
        val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_GET_RECORD}?repo=${enc(did)}&collection=${CollectionNsids.LEAFLET_POLL_DEFINITION}&rkey=${enc(rkey)}"
        val body = executeGet(urlStr)
        val record = json.parseToJsonElement(body).jsonObject
        val value = record["value"]?.jsonObject ?: throw IllegalStateException("Missing poll value")
        return json.decodeFromJsonElement(LeafletPollDefinition.serializer(), value)
    }

    suspend fun listPollVotes(did: String, pollRkey: String): List<LeafletPollVote> {
        val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.REPO_LIST_RECORDS}?repo=${enc(did)}&collection=${CollectionNsids.LEAFLET_POLL_VOTE}&limit=100"
        val body = executeGet(urlStr)
        val response = json.parseToJsonElement(body).jsonObject
        val records = response["records"]?.jsonArray.orEmpty()
        return records.mapNotNull { record ->
            val uri = record.jsonObject["uri"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val value = record.jsonObject["value"]?.jsonObject ?: return@mapNotNull null
            val vote = runCatching { json.decodeFromJsonElement(LeafletPollVote.serializer(), value) }.getOrNull()
                ?: return@mapNotNull null
            val rkey = AtUri.parse(uri)?.recordKey ?: return@mapNotNull null
            if (rkey != pollRkey) return@mapNotNull null
            vote
        }
    }

    suspend fun createPollVote(did: String, pollUri: String, options: List<String>): JsonObject {
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val authClient = atOAuth.createClient()
        return authClient.procedure(
            nsid = "com.atproto.repo.createRecord",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            input = buildJsonObject {
                put("repo", session.did)
                put("collection", CollectionNsids.LEAFLET_POLL_VOTE)
                put("record", buildJsonObject {
                    put("\$type", CollectionNsids.LEAFLET_POLL_VOTE)
                    put("poll", buildJsonObject {
                        put("uri", pollUri)
                    })
                    put("option", buildJsonArray { options.forEach { add(JsonPrimitive(it)) } })
                })
            },
            inputSerializer = JsonObject.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
    }

    private suspend fun resolvePdsUrl(did: String): String? {
        return try {
            val urlStr = "https://plc.directory/${enc(did)}"
            val body = json.parseToJsonElement(executeGet(urlStr)).jsonObject
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
