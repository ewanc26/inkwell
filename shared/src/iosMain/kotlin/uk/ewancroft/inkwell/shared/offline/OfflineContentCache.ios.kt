package uk.ewancroft.inkwell.shared.offline

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.posix.time

/** iOS caches full public records in Caches/, where the OS may safely purge them. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class OfflineContentCacheIos(
    cacheDirPath: String,
    private val policy: OfflineCachePolicy = OfflineCachePolicy(),
) : OfflineContentCache {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val cacheFilePath = "$cacheDirPath/$CACHE_FILENAME"

    override suspend fun upsert(record: CachedOfflineRecord) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val now = nowMillis()
            val records = readInternal().associateBy { it.uri }.toMutableMap()
            records[record.uri] = record.copy(cachedAtMillis = now, lastAccessedAtMillis = now)
            writeInternal(OfflineContentCacheRetention.retain(records.values, policy, now))
        }
    }

    override suspend fun load(uri: String): CachedOfflineRecord? = withContext(Dispatchers.Default) {
        mutex.withLock {
            val now = nowMillis()
            val records = readInternal()
            val record = records.firstOrNull { it.uri == uri } ?: return@withLock null
            if (OfflineContentCacheRetention.isExpired(record, policy, now)) {
                writeInternal(records.filterNot { it.uri == uri })
                return@withLock null
            }
            val accessed = record.copy(lastAccessedAtMillis = now)
            writeInternal(records.map { if (it.uri == uri) accessed else it })
            accessed
        }
    }

    override suspend fun loadAll(): List<CachedOfflineRecord> = withContext(Dispatchers.Default) {
        mutex.withLock {
            val retained = OfflineContentCacheRetention.retain(readInternal(), policy, nowMillis())
            writeInternal(retained)
            retained
        }
    }

    override suspend fun remove(uri: String) = withContext(Dispatchers.Default) {
        mutex.withLock { writeInternal(readInternal().filterNot { it.uri == uri }) }
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        mutex.withLock {
            val fileManager = NSFileManager.defaultManager
            if (fileManager.fileExistsAtPath(cacheFilePath)) {
                fileManager.removeItemAtPath(cacheFilePath, error = null)
            }
        }
    }

    private fun readInternal(): List<CachedOfflineRecord> {
        if (!NSFileManager.defaultManager.fileExistsAtPath(cacheFilePath)) return emptyList()
        return runCatching {
            val content = NSString.stringWithContentsOfFile(
                path = cacheFilePath,
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: return emptyList()
            json.decodeFromString<List<CachedOfflineRecord>>(content)
        }.getOrDefault(emptyList())
    }

    private fun writeInternal(records: List<CachedOfflineRecord>) {
        val jsonString = json.encodeToString(records)
        val bytes = jsonString.encodeToByteArray()
        bytes.usePinned {
            val data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            NSFileManager.defaultManager.createFileAtPath(
                path = cacheFilePath,
                contents = data,
                attributes = null,
            )
        }
    }

    private fun nowMillis(): Long = time(null) * 1_000

    private companion object {
        const val CACHE_FILENAME = "offline_content_cache.json"
    }
}
