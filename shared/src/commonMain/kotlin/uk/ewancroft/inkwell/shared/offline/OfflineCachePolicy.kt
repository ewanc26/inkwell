package uk.ewancroft.inkwell.shared.offline

import kotlinx.serialization.Serializable

/** The kind of content retained for offline reading. */
@Serializable
enum class OfflineCacheKind {
    Publication,
    Document,
    Image,
}

/**
 * A lightweight cache index entry. Platform implementations own the bytes;
 * this common model keeps eviction decisions consistent between the apps.
 */
@Serializable
data class OfflineCacheEntry(
    val key: String,
    val kind: OfflineCacheKind,
    val cachedAtMillis: Long,
    val lastAccessedAtMillis: Long,
    val estimatedByteCount: Long,
) {
    init {
        require(key.isNotBlank()) { "A cache entry needs a key." }
        require(cachedAtMillis >= 0) { "A cache timestamp cannot be negative." }
        require(lastAccessedAtMillis >= 0) { "An access timestamp cannot be negative." }
        require(estimatedByteCount >= 0) { "A cache entry size cannot be negative." }
    }
}

/** Retention limits used by platform-backed offline caches. */
@Serializable
data class OfflineCachePolicy(
    val maxEntries: Int = 500,
    val maxBytes: Long = 100L * 1024 * 1024,
    val metadataTtlMillis: Long = 30L * 24 * 60 * 60 * 1_000,
    val imageTtlMillis: Long = 14L * 24 * 60 * 60 * 1_000,
) {
    init {
        require(maxEntries > 0) { "The cache must allow at least one entry." }
        require(maxBytes > 0) { "The cache must allow at least one byte." }
        require(metadataTtlMillis > 0) { "Metadata retention must be positive." }
        require(imageTtlMillis > 0) { "Image retention must be positive." }
    }

    fun isExpired(entry: OfflineCacheEntry, nowMillis: Long): Boolean {
        require(nowMillis >= 0) { "The current timestamp cannot be negative." }
        val ttl = if (entry.kind == OfflineCacheKind.Image) imageTtlMillis else metadataTtlMillis
        return nowMillis - entry.cachedAtMillis >= ttl
    }

    /**
     * Returns keys to evict, first removing expired data and then least-recently
     * used entries until both configured limits are satisfied.
     */
    fun keysToEvict(entries: Collection<OfflineCacheEntry>, nowMillis: Long): Set<String> {
        val expired = entries.filter { isExpired(it, nowMillis) }
        val retained = entries.filterNot { it in expired }.toMutableList()
        val evictions = expired.mapTo(linkedSetOf()) { it.key }
        var bytes = retained.sumOf(OfflineCacheEntry::estimatedByteCount)
        var retainedCount = retained.size

        retained
            .sortedWith(compareBy<OfflineCacheEntry> { it.lastAccessedAtMillis }
            .thenBy { it.cachedAtMillis }
                .thenBy { it.key })
            .forEach { entry ->
                if (retainedCount <= maxEntries && bytes <= maxBytes) {
                    return@forEach
                }
                evictions += entry.key
                retainedCount -= 1
                bytes -= entry.estimatedByteCount
            }

        return evictions
    }
}

/** A pending mutation that must wait for authenticated network access. */
@Serializable
enum class SyncMutationKind {
    Subscribe,
    Unsubscribe,
    Recommend,
    Unrecommend,
    CreateComment,
}

/**
 * Platform queues persist these entries; platform API clients execute them
 * after reconnecting. The owning account, comment text, and reply target are
 * deliberately explicit rather than an opaque JSON payload so each can be
 * validated before it is stored.
 */
@Serializable
data class SyncQueueEntry(
    val id: String,
    val accountDid: String,
    val kind: SyncMutationKind,
    val subjectUri: String,
    val createdAtMillis: Long,
    val commentText: String? = null,
    val replyToUri: String? = null,
) {
    init {
        require(id.isNotBlank()) { "A queued mutation needs an identifier." }
        require(accountDid.isNotBlank()) { "A queued mutation needs an owning account." }
        require(subjectUri.isNotBlank()) { "A queued mutation needs a subject." }
        require(createdAtMillis >= 0) { "A queued mutation timestamp cannot be negative." }
        if (kind == SyncMutationKind.CreateComment) {
            require(!commentText.isNullOrBlank()) { "A queued comment needs text." }
        } else {
            require(commentText == null) { "Only queued comments may include text." }
            require(replyToUri == null) { "Only queued comments may include a reply target." }
        }
    }
}

/** Storage contract for platform-specific offline content caches. */
interface OfflineCacheIndex {
    suspend fun loadEntries(): List<OfflineCacheEntry>
    suspend fun upsert(entry: OfflineCacheEntry)
    suspend fun remove(keys: Set<String>)
    suspend fun clear()
}

/** Storage contract for mutations awaiting a reconnect. */
interface OfflineSyncQueue {
    suspend fun load(): List<SyncQueueEntry>
    suspend fun enqueue(entry: SyncQueueEntry)
    suspend fun remove(ids: Set<String>)
}

/** Bounded retention for locally pending, user-initiated mutations. */
object OfflineSyncQueueRetention {
    const val maxEntries: Int = 500
    const val maxAgeMillis: Long = 30L * 24 * 60 * 60 * 1_000

    fun retain(entries: Collection<SyncQueueEntry>, nowMillis: Long): List<SyncQueueEntry> {
        val latestById = linkedMapOf<String, SyncQueueEntry>()
        entries.forEach { latestById[it.id] = it }
        return latestById.values
            .asSequence()
            .filter { nowMillis - it.createdAtMillis < maxAgeMillis }
            .sortedWith(compareBy<SyncQueueEntry> { it.createdAtMillis }.thenBy { it.id })
            .toList()
            .takeLast(maxEntries)
    }
}
