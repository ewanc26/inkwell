package uk.ewancroft.inkwell.shared.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** JVM implementation of [FeedCache] backed by a JSON file. */
class FeedCacheJvm(cacheDirPath: String) : FeedCache {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val cacheFile = File(cacheDirPath, FEED_CACHE_FILENAME)

    override suspend fun save(items: List<CachedFeedItem>) = withContext(Dispatchers.IO) {
        mutex.withLock { write(json.encodeToString(FeedCacheRetention.retain(items))) }
    }

    override suspend fun upsert(items: List<CachedFeedItem>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val existing = read().associateBy { it.uri }.toMutableMap()
            items.forEach { existing[it.uri] = it }
            write(json.encodeToString(FeedCacheRetention.retain(existing.values)))
        }
    }

    override suspend fun remove(uri: String) = withContext(Dispatchers.IO) {
        mutex.withLock { write(json.encodeToString(FeedCacheRetention.retain(read().filter { it.uri != uri }))) }
    }

    override suspend fun load(limit: Int) = withContext(Dispatchers.IO) {
        mutex.withLock { read().sortedByDescending { it.cachedAt }.take(limit) }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { if (cacheFile.exists()) cacheFile.delete() }
    }

    private fun write(value: String) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.writeText(value)
    }

    private fun read(): List<CachedFeedItem> = try {
        if (!cacheFile.exists()) emptyList()
        else json.decodeFromString<List<CachedFeedItem>>(cacheFile.readText())
    } catch (_: Exception) {
        emptyList()
    }

    private companion object {
        const val FEED_CACHE_FILENAME = "feed_cache.json"
    }
}
