package uk.ewancroft.inkwell.shared.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation of [FeedCache] backed by a JSON file.
 *
 * All reads and writes are serialised by a [Mutex] so concurrent
 * coroutine callers never corrupt the file.
 *
 * @param cacheDirPath Absolute path to the cache directory (typically
 *   `context.cacheDir.absolutePath`).
 */
class FeedCacheAndroid(
    cacheDirPath: String
) : uk.ewancroft.inkwell.shared.feed.FeedCache {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val cacheFile = File(cacheDirPath, FEED_CACHE_FILENAME)

    override suspend fun save(items: List<CachedFeedItem>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            cacheFile.writeText(json.encodeToString(FeedCacheRetention.retain(items)))
        }
    }

    override suspend fun upsert(items: List<CachedFeedItem>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val existing = readInternal().associateBy { it.uri }.toMutableMap()
            for (item in items) {
                existing[item.uri] = item
            }
            cacheFile.writeText(json.encodeToString(FeedCacheRetention.retain(existing.values)))
        }
    }

    override suspend fun remove(uri: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val filtered = FeedCacheRetention.retain(readInternal().filter { it.uri != uri })
            cacheFile.writeText(json.encodeToString(filtered))
        }
    }

    override suspend fun load(limit: Int): List<CachedFeedItem> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readInternal().sortedByDescending { it.cachedAt }.take(limit)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (cacheFile.exists()) cacheFile.delete()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun readInternal(): List<CachedFeedItem> {
        if (!cacheFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<CachedFeedItem>>(cacheFile.readText())
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val FEED_CACHE_FILENAME = "feed_cache.json"
    }
}
