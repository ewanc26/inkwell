package uk.ewancroft.inkwell.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
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
    val body: JsonObject = json.decodeFromString(executeGet(urlStr))
    return body["did"]?.jsonPrimitive?.content
        ?: throw IllegalStateException("resolveHandle returned no did")
}

suspend fun PdsRepository.getProfile(did: String): BlueskyProfile {
    val urlStr = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.ACTOR_GET_PROFILE}?actor=${enc(did)}"
    return json.decodeFromString(executeGet(urlStr))
}

suspend fun PdsRepository.downloadBlob(cid: String, fromDID: String): ByteArray = withContext(Dispatchers.IO) {
    val pdsUrl = resolvePdsUrl(fromDID) ?: XrpcEndpoints.PUBLIC_BSKY_API
    val urlStr = "$pdsUrl${XrpcEndpoints.SYNC_GET_BLOB}?cid=${enc(cid)}&did=${enc(fromDID)}"
    val request = Request.Builder().url(urlStr).get().build()
    publicHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            throw java.io.IOException("Blob download failed: HTTP ${response.code}")
        }
        val body = response.body ?: throw java.io.IOException("Blob response had no body")
        readBoundedBlob(body)
    }
}

private fun readBoundedBlob(body: ResponseBody): ByteArray {
    if (body.contentLength() > MAX_READER_BLOB_BYTES) {
        throw java.io.IOException("Blob response exceeds the reader size limit")
    }
    val bytes = body.source().readByteArray(MAX_READER_BLOB_BYTES + 1)
    if (bytes.size.toLong() > MAX_READER_BLOB_BYTES) {
        throw java.io.IOException("Blob response exceeds the reader size limit")
    }
    return bytes
}
