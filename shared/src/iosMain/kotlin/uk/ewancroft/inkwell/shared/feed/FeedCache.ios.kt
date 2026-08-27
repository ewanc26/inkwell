package uk.ewancroft.inkwell.shared.feed

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile

/**
 * iOS implementation of [FeedCache] backed by a JSON file.
 *
 * All reads and writes are serialised by a [Mutex] so concurrent
 * coroutine callers never corrupt the file.
 *
 * @param cacheDirPath Absolute path to the caches directory (typically
 *   `NSFileManager.defaultManager.cacheDirectoryPath`).
 */
@OptIn(ExperimentalForeignApi::class)
class FeedCacheIos(
    cacheDirPath: String
) : uk.ewancroft.inkwell.shared.feed.FeedCache {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val cacheFilePath = "$cacheDirPath/$FEED_CACHE_FILENAME"

    override suspend fun save(items: List<CachedFeedItem>) = withContext(Dispatchers.Default) {
        mutex.withLock {
            writeToFile(json.encodeToString(items))
        }
    }

    override suspend fun upsert(items: List<CachedFeedItem>) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val existing = readFromFile().associateBy { it.uri }.toMutableMap()
            for (item in items) {
                existing[item.uri] = item
            }
            writeToFile(json.encodeToString(existing.values.toList()))
        }
    }

    override suspend fun remove(uri: String) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val filtered = readFromFile().filter { it.uri != uri }
            writeToFile(json.encodeToString(filtered))
        }
    }

    override suspend fun load(limit: Int): List<CachedFeedItem> = withContext(Dispatchers.Default) {
        mutex.withLock {
            readFromFile().sortedByDescending { it.cachedAt }.take(limit)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        mutex.withLock {
            val fm = NSFileManager.defaultManager
            if (fm.fileExistsAtPath(cacheFilePath)) {
                fm.removeItemAtPath(cacheFilePath, error = null)
            }
        }
    }

    // ── File I/O ──────────────────────────────────────────────────────────

    private fun writeToFile(jsonString: String) {
        val nsString = jsonString as NSString
        val bytes = jsonString.encodeToByteArray()
        bytes.usePinned {
            val data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            NSFileManager.defaultManager.createFileAtPath(
                path = cacheFilePath,
                contents = data,
                attributes = null
            )
        }
    }

    private fun readFromFile(): List<CachedFeedItem> {
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(cacheFilePath)) return emptyList()
        return try {
            val content = NSString.stringWithContentsOfFile(
                path = cacheFilePath,
                encoding = NSUTF8StringEncoding,
                error = null
            ) ?: return emptyList()
            json.decodeFromString<List<CachedFeedItem>>(content as String)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val FEED_CACHE_FILENAME = "feed_cache.json"
    }
}
