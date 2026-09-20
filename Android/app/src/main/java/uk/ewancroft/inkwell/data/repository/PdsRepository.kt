package uk.ewancroft.inkwell.data.repository

import io.github.kikin81.atproto.oauth.AtOAuth
import io.github.kikin81.atproto.oauth.OAuthSessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.HttpUrl
import okhttp3.Request
import uk.ewancroft.inkwell.TestingConfig
import uk.ewancroft.inkwell.TestingModeException
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.model.UserLexicon
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import uk.ewancroft.inkwell.shared.policy.RecordListPolicy
import uk.ewancroft.inkwell.shared.validation.JsonSafety
import java.net.URLEncoder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class UserSessionInfo(
    val handle: String,
    val did: String,
    val pdsUrl: String,
)

internal object RateLimitRetryPolicy {
    const val MAX_DELAY_MS = 60_000L

    fun delayMillis(header: String?, attempt: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val value = header?.trim().orEmpty()
        value.toLongOrNull()?.takeIf { it >= 0 }?.let {
            // Cap before converting seconds to milliseconds so an untrusted
            // header cannot overflow Long during the multiplication.
            return it.coerceAtMost(MAX_DELAY_MS / 1000) * 1000
        }
        runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                .toInstant().toEpochMilli() - nowMillis
        }.getOrNull()?.takeIf { it >= 0 }?.let { return it.coerceAtMost(MAX_DELAY_MS) }
        return (100L shl attempt.coerceIn(0, 9)).coerceAtMost(MAX_DELAY_MS)
    }
}

internal fun rateLimitOrigin(url: HttpUrl): String {
    val defaultPort = (url.scheme == "https" && url.port == 443)
        || (url.scheme == "http" && url.port == 80)
    return if (defaultPort) "${url.scheme}://${url.host}" else "${url.scheme}://${url.host}:${url.port}"
}

internal object PdsResponseBodyReader {
    fun read(input: InputStream, contentLength: Long?, maxBodyBytes: Int): String {
        if (contentLength != null && contentLength > maxBodyBytes) {
            throw IOException("PDS response exceeded the ${maxBodyBytes}-byte safety budget")
        }
        input.use {
            val output = ByteArrayOutputStream(minOf(maxBodyBytes, 64 * 1024))
            val buffer = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = it.read(buffer)
                if (read == -1) break
                total += read.toLong()
                if (total > maxBodyBytes.toLong()) {
                    throw IOException("PDS response exceeded the ${maxBodyBytes}-byte safety budget")
                }
                output.write(buffer, 0, read)
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }
}

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
    private val rateLimitCooldowns = mutableMapOf<String, Long>()

    suspend fun getSession(): UserSessionInfo? {
        val session = sessionStore.load() ?: return null
        val pdsUrl = session.pdsUrl ?: return null
        return UserSessionInfo(
            handle = session.handle ?: session.did.orEmpty(),
            did = session.did.orEmpty(),
            pdsUrl = pdsUrl,
        )
    }

    /**
     * Revalidates a restored OAuth session against the PDS before the UI uses
     * it. The DID returned by the authenticated endpoint is authoritative;
     * the mutable stored handle is never used as the account identity.
     */
    suspend fun validateRestoredSession(): UserSessionInfo? {
        val stored = sessionStore.load() ?: return null
        val did = stored.did ?: return null
        val pdsUrl = stored.pdsUrl ?: return null
        val response = atOAuth.createClient().query(
            nsid = "com.atproto.server.getSession",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            responseSerializer = JsonObject.serializer(),
        )
        val returnedDid = response["did"]?.jsonPrimitive?.contentOrNull
        check(restoredSessionMatches(storedDid = did, returnedDid = returnedDid)) {
            "OAuth session subject did not match the stored account"
        }
        return UserSessionInfo(
            handle = response["handle"]?.jsonPrimitive?.contentOrNull
                ?: stored.handle
                ?: did,
            did = did,
            pdsUrl = pdsUrl,
        )
    }

    /** URL-encodes a query param value — DIDs/AT-URIs can contain `:`, `/`,
     *  `.`, and other characters that must not be interpolated raw into a
     *  query string. */
    internal fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Parses untrusted PDS JSON only after applying structural safety limits. */
    internal inline fun <reified T> decodeSafe(body: String): T {
        val element = json.parseToJsonElement(body)
        check(JsonSafety.isSafe(element)) { "PDS JSON exceeded structural safety limits" }
        return json.decodeFromJsonElement(element)
    }

    /** Executes a GET and returns the raw body, throwing on non-2xx statuses
     *  or a missing body instead of decoding an error payload as success. */
    internal suspend fun executeGet(urlStr: String, maxBodyBytes: Int = DEFAULT_RESPONSE_BYTES): String {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder().url(urlStr).get().build()
            val origin = rateLimitOrigin(request.url)
            var attempt = 0
            var result: String? = null
            while (result == null) {
                val cooldown = synchronized(rateLimitCooldowns) {
                    (rateLimitCooldowns[origin] ?: 0L) - System.currentTimeMillis()
                }
                if (cooldown > 0) delay(cooldown)
                val response = publicHttpClient.newCall(request).execute()
                if (response.code == 429 && attempt < MAX_RATE_LIMIT_ATTEMPTS - 1) {
                    val delayMs = RateLimitRetryPolicy.delayMillis(response.header("Retry-After"), attempt)
                    response.close()
                    synchronized(rateLimitCooldowns) {
                        val until = System.currentTimeMillis() + delayMs
                        rateLimitCooldowns[origin] = maxOf(rateLimitCooldowns[origin] ?: 0L, until)
                    }
                    attempt += 1
                    continue
                }
                result = response.use {
                    if (!response.isSuccessful) {
                        throw java.io.IOException("PDS request failed: HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IOException("PDS request returned no body")
                    PdsResponseBodyReader.read(body.byteStream(), body.contentLength(), maxBodyBytes)
                }
            }
            result ?: error("PDS request completed without a response body")
        }
    }

    private companion object {
        const val MAX_RATE_LIMIT_ATTEMPTS = 3
        const val DEFAULT_RESPONSE_BYTES = 2 * 1024 * 1024
        const val MAX_RECORD_PAGES = 10_000
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
        val pageBudget = (DEFAULT_RESPONSE_BYTES + limit.coerceIn(1, 100) * 8 * 1024).coerceAtMost(8 * 1024 * 1024)
        return decodeSafe(executeGet(urlStr, pageBudget))
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
        return decodeSafe(executeGet(urlStr))
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
        val response = authClient.procedure(
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
        return response
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

        val contentType = io.ktor.http.ContentType.parse(mimeType)

        val response = authClient.procedure(
            nsid = "com.atproto.repo.uploadBlob",
            params = Unit,
            paramsSerializer = Unit.serializer(),
            bytes,
            contentType,
            JsonObject.serializer(),
        )
        validateUploadBlobResponse(response, mimeType, bytes.size.toLong())
        return response
    }

    suspend fun deleteRecord(collection: String, rkey: String, swapRecord: String? = null) {
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
            input = deleteRecordInput(session.did.orEmpty(), collection, rkey, swapRecord),
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
        maxRecords: Int = Int.MAX_VALUE,
    ): List<RawRecordEntry> {
        val all = mutableListOf<RawRecordEntry>()
        var cursor: String? = null
        var pageCount = 0
        do {
            check(pageCount++ < MAX_RECORD_PAGES) { "PDS pagination exceeded the safety page budget" }
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
            val body = decodeSafe<JsonObject>(executeGet(urlStr))
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

internal fun validateUploadBlobResponse(response: JsonObject, mimeType: String, byteCount: Long) {
    val blob = response["blob"]?.jsonObject
        ?: throw IllegalStateException("Blob upload response did not contain a blob")
    check(blob["mimeType"]?.jsonPrimitive?.content == mimeType) {
        "Blob upload response MIME type did not match request"
    }
    check(blob["size"]?.jsonPrimitive?.long == byteCount) {
        "Blob upload response size did not match request"
    }
    check(blob["ref"]?.jsonObject?.get("\$link")?.jsonPrimitive?.content?.isNotBlank() == true) {
        "Blob upload response did not contain a CID"
    }
}

internal fun restoredSessionMatches(storedDid: String, returnedDid: String?): Boolean =
    returnedDid != null && returnedDid == storedDid

internal fun deleteRecordInput(
    repo: String,
    collection: String,
    rkey: String,
    swapRecord: String?,
): JsonObject = buildJsonObject {
    put("repo", repo)
    put("collection", collection)
    put("rkey", rkey)
    swapRecord?.let { put("swapRecord", it) }
}
