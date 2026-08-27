package uk.ewancroft.inkwell.shared.offline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** JVM backend used by desktop tooling and shared-core tests. */
class OfflineContentCacheJvm(
    cacheDirPath: String,
    private val policy: OfflineCachePolicy = OfflineCachePolicy(),
) : OfflineContentCache {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val cacheFile = File(cacheDirPath, CACHE_FILENAME)

    override suspend fun upsert(record: CachedOfflineRecord) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val records = readInternal().associateBy { it.uri }.toMutableMap()
            records[record.uri] = record.copy(cachedAtMillis = now, lastAccessedAtMillis = now)
            writeInternal(OfflineContentCacheRetention.retain(records.values, policy, now))
        }
    }

    override suspend fun load(uri: String): CachedOfflineRecord? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val now = System.currentTimeMillis()
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

    override suspend fun loadAll(): List<CachedOfflineRecord> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val retained = OfflineContentCacheRetention.retain(readInternal(), policy, System.currentTimeMillis())
            writeInternal(retained)
            retained
        }
    }

    override suspend fun remove(uri: String) = withContext(Dispatchers.IO) {
        mutex.withLock { writeInternal(readInternal().filterNot { it.uri == uri }) }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { if (cacheFile.exists()) cacheFile.delete() }
    }

    private fun readInternal(): List<CachedOfflineRecord> {
        if (!cacheFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<List<CachedOfflineRecord>>(cacheFile.readText())
        }.getOrDefault(emptyList())
    }

    private fun writeInternal(records: List<CachedOfflineRecord>) {
        cacheFile.writeText(json.encodeToString(records))
    }

    private companion object {
        const val CACHE_FILENAME = "offline_content_cache.json"
    }
}
