package uk.ewancroft.inkwell.shared.feed

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uk.ewancroft.inkwell.shared.offline.OfflineCacheEntry
import uk.ewancroft.inkwell.shared.offline.OfflineCacheKind
import uk.ewancroft.inkwell.shared.offline.OfflineCachePolicy

/**
 * Shared eviction policy for the persisted reader-feed snapshot.
 *
 * Feed items are document metadata, so they use the same 30-day, 500-entry,
 * 100 MB metadata limits as the full-record cache. Platform backends only
 * perform file I/O; this object keeps their retention behaviour identical.
 */
object FeedCacheRetention {
    private val defaultPolicy = OfflineCachePolicy()
    private val sizeJson = Json { encodeDefaults = true }

    fun retain(
        items: Collection<CachedFeedItem>,
        nowMillis: Long = currentTimeMillis(),
        policy: OfflineCachePolicy = defaultPolicy,
    ): List<CachedFeedItem> {
        val newestByUri = linkedMapOf<String, CachedFeedItem>()
        items.forEach { item ->
            val existing = newestByUri[item.uri]
            if (existing == null || item.cachedAt >= existing.cachedAt) {
                newestByUri[item.uri] = item
            }
        }

        val evictedUris = policy.keysToEvict(
            newestByUri.values.map { it.toOfflineCacheEntry() },
            nowMillis,
        )
        return newestByUri.values.filterNot { it.uri in evictedUris }
    }

    private fun CachedFeedItem.toOfflineCacheEntry(): OfflineCacheEntry = OfflineCacheEntry(
        key = uri,
        kind = OfflineCacheKind.Document,
        cachedAtMillis = cachedAt,
        lastAccessedAtMillis = cachedAt,
        estimatedByteCount = sizeJson.encodeToString(this).encodeToByteArray().size.toLong(),
    )
}
