package uk.ewancroft.inkwell.shared.offline

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Android JSON-file implementation of the pending mutation queue. */
class OfflineSyncQueueAndroid(cacheDirPath: String) : OfflineSyncQueue {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val queueFile = File(cacheDirPath, QUEUE_FILENAME)

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

    private fun readInternal(): List<SyncQueueEntry> = runCatching {
        if (!queueFile.exists()) emptyList()
        else json.decodeFromString<List<SyncQueueEntry>>(queueFile.readText())
    }.getOrDefault(emptyList())

    private fun writeInternal(entries: List<SyncQueueEntry>) {
        queueFile.parentFile?.mkdirs()
        queueFile.writeText(json.encodeToString(entries))
    }

    private companion object {
        const val QUEUE_FILENAME = "offline_sync_queue.json"
    }
}
