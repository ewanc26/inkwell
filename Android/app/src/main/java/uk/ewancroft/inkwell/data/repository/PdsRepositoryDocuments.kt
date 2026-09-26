package uk.ewancroft.inkwell.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import okhttp3.ResponseBody
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.model.common.BlobRef
import uk.ewancroft.inkwell.shared.graph.CollectionNsids
import uk.ewancroft.inkwell.shared.util.HandleUtils
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints

private const val MAX_READER_BLOB_BYTES = 10L * 1024 * 1024

suspend fun PdsRepository.fetchDocuments(did: String, pdsUrl: String? = null): List<JsonObject> =
    fetchDocumentEntries(did, pdsUrl).map { it.value }

data class DocumentRecordEntry(val uri: String, val value: JsonObject)

suspend fun PdsRepository.fetchDocumentEntries(did: String, pdsUrl: String? = null): List<DocumentRecordEntry> =
    listAllRecords(did, CollectionNsids.DOCUMENT, pdsUrl).map { DocumentRecordEntry(it.uri, it.value) }

suspend fun PdsRepository.resolveHandle(handle: String): String {
    val normalized = HandleUtils.normalize(handle)
    val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.IDENTITY_RESOLVE_HANDLE}?handle=${enc(normalized)}"
    val body: JsonObject = decodeSafe(executeGet(urlStr))
    return body["did"]?.jsonPrimitive?.content
        ?: throw IllegalStateException("resolveHandle returned no did")
}

suspend fun PdsRepository.getProfile(did: String): BlueskyProfile {
    val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.ACTOR_GET_PROFILE}?actor=${enc(did)}"
    return decodeSafe(executeGet(urlStr))
}

suspend fun PdsRepository.downloadBlob(
    cid: String,
    fromDID: String,
    declaredSize: Long? = null,
    expectedMimeType: String? = null
): ByteArray = withContext(Dispatchers.IO) {
    validateDeclaredBlobSize(declaredSize)
    val pdsUrl = resolvePdsUrl(fromDID)
    val urlStr = "$pdsUrl${XrpcEndpoints.SYNC_GET_BLOB}?cid=${enc(cid)}&did=${enc(fromDID)}"
    val request = Request.Builder().url(urlStr).get().build()
    publicHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw java.io.IOException("Blob download failed: HTTP ${response.code}")
        }
        val body = response.body ?: throw java.io.IOException("Blob response had no body")
        validateBlobContentType(body.contentType()?.toString(), expectedMimeType)
        readBoundedBlob(body)
    }
}

/**
 * Downloads the blob a record field points at, given the field's raw JSON.
 *
 * Blob-backed long-form content (Markpub `text.textBlob`, Leaflet `blobPages`) is
 * read by both the Reader and the Writer, so the ref decoding lives here rather
 * than in each caller. [BlobRef] already tolerates the canonical
 * `{"ref": {"$link": …}}` shape and the legacy top-level `{"$link": …}` one.
 */
suspend fun PdsRepository.downloadBlobRef(
    blob: JsonObject,
    fromDID: String,
    expectedMimeType: String? = null,
): ByteArray {
    val ref = runCatching { blobRefJson.decodeFromJsonElement<BlobRef>(blob) }.getOrNull()
    val cid = ref?.link?.takeIf { it.isNotEmpty() }
        ?: throw java.io.IOException("Blob reference had no CID")
    return downloadBlob(
        cid = cid,
        fromDID = fromDID,
        declaredSize = ref.size.takeIf { it > 0 }?.toLong(),
        expectedMimeType = expectedMimeType,
    )
}

private val blobRefJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal fun validateBlobContentType(actual: String?, expected: String?) {
    if (expected == null) return
    if (actual == null) {
        throw java.io.IOException("Blob response omitted the expected MIME type $expected")
    }
    val actualType = actual.substringBefore(';').trim().lowercase()
    val expectedType = expected.substringBefore(';').trim().lowercase()
    if (actualType != expectedType) {
        throw java.io.IOException("Blob response MIME type $actualType does not match expected $expectedType")
    }
}

internal fun validateDeclaredBlobSize(declaredSize: Long?) {
    if (declaredSize != null && (declaredSize < 0 || declaredSize > MAX_READER_BLOB_BYTES)) {
        throw java.io.IOException("Blob declaration exceeds the reader size limit")
    }
}

internal fun readBoundedBlob(body: ResponseBody): ByteArray {
    if (body.contentLength() > MAX_READER_BLOB_BYTES) {
        throw java.io.IOException("Blob response exceeds the reader size limit")
    }
    val source = body.source()
    val buffer = Buffer()
    var total = 0L
    while (total <= MAX_READER_BLOB_BYTES) {
        val read = source.read(buffer, minOf(16 * 1024L, MAX_READER_BLOB_BYTES + 1 - total))
        if (read == -1L) break
        total += read
    }
    if (total > MAX_READER_BLOB_BYTES) {
        throw java.io.IOException("Blob response exceeds the reader size limit")
    }
    return buffer.readByteArray()
}
