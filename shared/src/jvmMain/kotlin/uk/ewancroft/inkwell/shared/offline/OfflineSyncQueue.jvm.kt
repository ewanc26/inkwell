package uk.ewancroft.inkwell.shared.offline

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** JVM implementation used by shared tests and desktop tooling. */
class OfflineSyncQueueJvm(durableDirPath: String, legacyCacheDirPath: String) : OfflineSyncQueue {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val queueFile = File(durableDirPath, QUEUE_FILENAME)
    private val legacyQueueFile = File(legacyCacheDirPath, QUEUE_FILENAME)

    override suspend fun load(): List<SyncQueueEntry> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val retained = OfflineSyncQueueRetention.retain(readInternal(), System.currentTimeMillis())
            writeInternal(retained)
            retained
        }
    }

    override suspend fun enqueue(entry: SyncQueueEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val entries = readInternal().filterNot { it.id == entry.id } + entry
            writeInternal(OfflineSyncQueueRetention.retain(entries, System.currentTimeMillis()))
        }
    }

    override suspend fun remove(ids: Set<String>) = withContext(Dispatchers.IO) {
        mutex.withLock { writeInternal(readInternal().filterNot { it.id in ids }) }
    }

    private fun readInternal(): List<SyncQueueEntry> = runCatching {
        migrateLegacyIfNeeded()
        if (!queueFile.exists()) emptyList()
        else decodeEntries(queueFile.readText())
    }.getOrThrow()

    private fun decodeEntries(raw: String): List<SyncQueueEntry> = runCatching {
        json.decodeFromString<SyncQueueFile>(raw).entries
    }.getOrElse { json.decodeFromString<List<SyncQueueEntry>>(raw) }

    private fun writeInternal(entries: List<SyncQueueEntry>) {
        queueFile.parentFile?.mkdirs()
        val temporary = File(queueFile.parentFile, "$QUEUE_FILENAME.tmp")
        temporary.writeText(json.encodeToString(SyncQueueFile(entries = entries)))
        Files.move(temporary.toPath(), queueFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    private fun migrateLegacyIfNeeded() {
        if (!queueFile.exists() && legacyQueueFile.exists()) {
            queueFile.parentFile?.mkdirs()
            Files.move(legacyQueueFile.toPath(), queueFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private companion object {
        const val QUEUE_FILENAME = "offline_sync_queue.json"
    }
}
