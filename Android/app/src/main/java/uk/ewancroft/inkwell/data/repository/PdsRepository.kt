package uk.ewancroft.inkwell.data.repository

import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import uk.ewancroft.inkwell.TestingConfig
import uk.ewancroft.inkwell.TestingModeException
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.model.UserLexicon
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.shared.policy.RecordListPolicy
import java.net.URLEncoder
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
    internal val atOAuth: AtOAuth,
    internal val sessionStore: OAuthSessionStore,
) {
    internal val json = Json { ignoreUnknownKeys = true; isLenient = true }

    internal val publicHttpClient = OkHttpClient.Builder()
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
    internal fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Executes a GET and returns the raw body, throwing on non-2xx statuses
     *  or a missing body instead of decoding an error payload as success. */
    internal suspend fun executeGet(urlStr: String): String = withContext(Dispatchers.IO) {
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
        if (TestingConfig.enabled) {
            TestingConfig.report("Create $collection record")
            throw TestingModeException("Create $collection record")
        }
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

    suspend fun createUserLexicon(user: Boolean = true): JsonObject {
        if (TestingConfig.enabled) {
            TestingConfig.report("Create ${CollectionNsids.USER} record")
            throw TestingModeException("Create ${CollectionNsids.USER} record")
        }
        val session = sessionStore.load() ?: throw Exception("Not authenticated")
        val authClient = atOAuth.createClient()
        return authClient.procedure(
            nsid = "com.atproto.repo.createRecord",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            input = buildJsonObject {
                put("repo", session.did)
                put("collection", CollectionNsids.USER)
                put("record", buildJsonObject {
                    put("user", user)
                    put("app", UserLexicon.CANONICAL_APP_URI)
                })
            },
            inputSerializer = JsonObject.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
    }

    /**
     * Returns the record key of the signed-in user's `uk.ewancroft.inkwell.user`
     * record, or null if none exists.
     */
    suspend fun getUserLexiconRkey(): String? {
        val session = sessionStore.load() ?: return null
        val did = session.did ?: return null
        val response = listRecords(
            did = did,
            collection = CollectionNsids.USER,
            limit = 1,
        )
        val records = response["records"]?.jsonArray ?: return null
        return records.firstOrNull()
            ?.jsonObject
            ?.get("uri")
            ?.jsonPrimitive
            ?.content
            ?.let { AtUri.parse(it)?.recordKey }
    }

    /** Deletes the signed-in user's Inkwell-user record, if one exists. */
    suspend fun deleteUserLexicon() {
        if (TestingConfig.enabled) {
            TestingConfig.report("Delete ${CollectionNsids.USER} record")
            throw TestingModeException("Delete ${CollectionNsids.USER} record")
        }
        val session = sessionStore.load() ?: return
        val rkey = getUserLexiconRkey() ?: return
        deleteRecord(CollectionNsids.USER, rkey)
    }

    suspend fun updateRecord(
        uri: String,
        record: JsonObject,
        revision: String,
    ): JsonObject {
        if (TestingConfig.enabled) {
            TestingConfig.report("Update record")
            throw TestingModeException("Update record")
        }
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
        if (TestingConfig.enabled) {
            TestingConfig.report("Upload image")
            throw TestingModeException("Upload image")
        }
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

    suspend fun deleteRecord(collection: String, rkey: String) {
        if (TestingConfig.enabled) {
            TestingConfig.report("Delete $collection record")
            throw TestingModeException("Delete $collection record")
        }
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

    // ── Pagination helper ────────────────────────────────────────────────

    internal data class RawRecordEntry(val uri: String, val value: JsonObject)

    /**
     * Paginates `com.atproto.repo.listRecords` to completion. Capped at
     * [maxRecords] so a misbehaving PDS returning an endless cursor can't
     * hang the caller forever.
     */
    internal suspend fun listAllRecords(
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

    internal suspend fun resolvePdsUrl(did: String): String? {
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
