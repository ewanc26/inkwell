package uk.ewancroft.inkwell.data.repository

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids

// ── Comments (pub.leaflet.comment) ─────────────────────────────────────

data class CommentEntry(val uri: String, val rkey: String, val comment: uk.ewancroft.inkwell.data.model.graph.LeafletComment)

/** Creates a `pub.leaflet.comment` record. */
suspend fun PdsRepository.createComment(
    subject: String,
    plaintext: String,
    replyTo: String? = null,
    onPage: String? = null
): JsonObject {
    val record = buildJsonObject {
        put("\$type", CollectionNsids.LEAFLET_COMMENT)
        put("subject", subject)
        put("plaintext", plaintext)
        if (replyTo != null) {
            put("reply", buildJsonObject { put("parent", replyTo) })
        }
        if (onPage != null) put("onPage", onPage)
    }
    return createRecord(CollectionNsids.LEAFLET_COMMENT, record)
}

/** Lists the signed-in user's own comments (paginated to completion). */
suspend fun PdsRepository.fetchComments(did: String, pdsUrl: String? = null): List<CommentEntry> =
    listAllRecords(did, CollectionNsids.LEAFLET_COMMENT, pdsUrl).mapNotNull { entry ->
        try {
            val commentJson = entry.value
            val comment = json.decodeFromJsonElement(uk.ewancroft.inkwell.data.model.graph.LeafletComment.serializer(), commentJson)
            val rkey = AtUri.parse(entry.uri)?.recordKey ?: return@mapNotNull null
            CommentEntry(entry.uri, rkey, comment)
        } catch (_: Exception) { null }
    }

/** Deletes a comment record by its record key. */
suspend fun PdsRepository.deleteComment(rkey: String) = deleteRecord(CollectionNsids.LEAFLET_COMMENT, rkey)
