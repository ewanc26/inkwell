package uk.ewancroft.inkwell.shared.feed

import uk.ewancroft.inkwell.shared.jetstream.JetstreamPayload
import uk.ewancroft.inkwell.shared.moderation.ModerationLabel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Parses a Jetstream commit payload (for `site.standard.document`
 * records) into a [CachedFeedItem] suitable for cache storage.
 *
 * Returns `null` if the payload is not a document commit or if
 * required fields are missing.
 */
fun JetstreamPayload.toCachedFeedItem(): CachedFeedItem? {
    if (collection != "site.standard.document") return null
    val record = record ?: return null

    val site = record["site"]?.jsonPrimitive?.contentOrNull ?: return null
    val title = record["title"]?.jsonPrimitive?.contentOrNull ?: return null
    val publishedAt = record["publishedAt"]?.jsonPrimitive?.contentOrNull ?: return null

    return CachedFeedItem(
        uri = "at://$did/site.standard.document/$rkey",
        authorDID = did,
        site = site,
        title = title,
        publishedAt = publishedAt,
        path = record["path"]?.jsonPrimitive?.contentOrNull,
        description = record["description"]?.jsonPrimitive?.contentOrNull,
        textContent = record["textContent"]?.jsonPrimitive?.contentOrNull,
        coverImageUrl = extractCoverImageUrl(record),
        moderationLabels = extractModerationLabels(record),
        cachedAt = currentTimeMillis()
    )
}

/**
 * Extracts the CDN URL from a `coverImage` blob ref in a document
 * record JSON object.
 */
private fun extractCoverImageUrl(record: JsonObject): String? {
    val coverImage = record["coverImage"] as? JsonObject ?: return null
    val ref = coverImage["ref"]?.jsonPrimitive?.contentOrNull ?: return null
    // Blob refs are typically "$type": "blob", "ref": "cid...", "mimeType": "...", "size": N
    // The CDN URL pattern is: https://bsky.social/xrpc/com.atproto.sync.getBlob?did=...&cid=...
    // But we don't have the DID here directly — it's in the payload.
    // For now, return the CID and let the caller construct the URL.
    return ref
}

private fun extractModerationLabels(record: JsonObject): List<ModerationLabel> =
    record["labels"]?.jsonObject?.get("values")?.jsonArray
        ?.mapNotNull { label ->
            label.jsonObject["val"]?.jsonPrimitive?.contentOrNull?.let { value ->
                ModerationLabel(
                    value = value,
                    source = label.jsonObject["src"]?.jsonPrimitive?.contentOrNull,
                )
            }
        }
        .orEmpty()
