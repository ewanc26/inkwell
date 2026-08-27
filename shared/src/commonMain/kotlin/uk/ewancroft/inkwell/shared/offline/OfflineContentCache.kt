package uk.ewancroft.inkwell.shared.offline

import kotlinx.serialization.Serializable

/**
 * A serialized publication or document record retained for offline reading.
 * The JSON is the record value rather than an authenticated XRPC response, so
 * it never includes session material and can be decoded by each native app's
 * existing Standard.site models.
 */
@Serializable
data class CachedOfflineRecord(
    val uri: String,
    val kind: OfflineCacheKind,
    val authorDid: String,
    val cid: String? = null,
    val recordJson: String,
    val cachedAtMillis: Long,
    val lastAccessedAtMillis: Long = cachedAtMillis,
) {
    init {
        require(uri.isNotBlank()) { "An offline record needs an AT URI." }
        require(authorDid.isNotBlank()) { "An offline record needs an author DID." }
        require(kind != OfflineCacheKind.Image) { "Images belong in the platform image cache." }
        require(recordJson.isNotBlank()) { "An offline record needs JSON." }
        require(cachedAtMillis >= 0) { "A cache timestamp cannot be negative." }
        require(lastAccessedAtMillis >= 0) { "An access timestamp cannot be negative." }
    }

    fun cacheEntry(): OfflineCacheEntry = OfflineCacheEntry(
        key = uri,
        kind = kind,
        cachedAtMillis = cachedAtMillis,
        lastAccessedAtMillis = lastAccessedAtMillis,
        estimatedByteCount = recordJson.encodeToByteArray().size.toLong(),
    )
}

/** Local storage contract for full records that have been opened before. */
interface OfflineContentCache {
    suspend fun upsert(record: CachedOfflineRecord)
    suspend fun load(uri: String): CachedOfflineRecord?
    suspend fun loadAll(): List<CachedOfflineRecord>
    suspend fun remove(uri: String)
    suspend fun clear()
}

/**
 * Common eviction logic used by the Android, iOS, and JVM file backends.
 * Platform implementations only provide locking, clock access, and file I/O.
 */
object OfflineContentCacheRetention {
    fun retain(
        records: Collection<CachedOfflineRecord>,
        policy: OfflineCachePolicy,
        nowMillis: Long,
    ): List<CachedOfflineRecord> {
        val latestByUri = linkedMapOf<String, CachedOfflineRecord>()
        records.forEach { record -> latestByUri[record.uri] = record }
        val evicted = policy.keysToEvict(
            entries = latestByUri.values.map(CachedOfflineRecord::cacheEntry),
            nowMillis = nowMillis,
        )
        return latestByUri.values.filterNot { it.uri in evicted }
    }

    fun isExpired(
        record: CachedOfflineRecord,
        policy: OfflineCachePolicy,
        nowMillis: Long,
    ): Boolean = policy.isExpired(record.cacheEntry(), nowMillis)
}
