package uk.ewancroft.inkwell.shared.offline

import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Android JSON-file implementation of the pending mutation queue. */
class OfflineSyncQueueAndroid(durableDirPath: String, legacyCacheDirPath: String) : OfflineSyncQueue {
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
        mutex.withLock {
            writeInternal(readInternal().filterNot { it.id in ids })
        }
    }

    private fun readInternal(): List<SyncQueueEntry> {
        migrateLegacyIfNeeded()
        if (!queueFile.exists()) return emptyList()
        return try {
            decodeEntries(queueFile.readText())
        } catch (error: Throwable) {
            preserveCorruptQueue()
            throw error
        }
    }

    private fun preserveCorruptQueue() {
        val preserved = File(queueFile.parentFile, "$QUEUE_FILENAME.corrupt-${System.currentTimeMillis()}")
        runCatching { queueFile.copyTo(preserved, overwrite = false) }
        // Leave the preserved artifact for recovery, but remove the active
        // path so the next load can start from an empty queue instead of
        // repeatedly failing on the same corrupt bytes.
        queueFile.delete()
    }

    private fun decodeEntries(raw: String): List<SyncQueueEntry> = runCatching {
        json.decodeFromString<SyncQueueFile>(raw).entries
    }.getOrElse { json.decodeFromString<List<SyncQueueEntry>>(raw) }

    private fun writeInternal(entries: List<SyncQueueEntry>) {
        queueFile.parentFile?.mkdirs()
        val temporary = File(queueFile.parentFile, "$QUEUE_FILENAME.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(json.encodeToString(SyncQueueFile(entries = entries)).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        val backup = File(queueFile.parentFile, "$QUEUE_FILENAME.bak")
        if (queueFile.exists()) check(queueFile.renameTo(backup)) { "Unable to stage offline sync queue replacement" }
        if (!temporary.renameTo(queueFile)) {
            backup.takeIf { it.exists() }?.renameTo(queueFile)
            error("Unable to replace offline sync queue")
        }
        backup.delete()
    }

    private fun migrateLegacyIfNeeded() {
        if (!queueFile.exists() && legacyQueueFile.exists()) {
            queueFile.parentFile?.mkdirs()
            check(legacyQueueFile.renameTo(queueFile)) { "Unable to migrate offline sync queue" }
        }
    }

    private companion object {
        const val QUEUE_FILENAME = "offline_sync_queue.json"
    }
}
