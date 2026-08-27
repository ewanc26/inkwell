package uk.ewancroft.inkwell.shared.offline

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
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

/** iOS JSON-file implementation of the pending mutation queue. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class OfflineSyncQueueIos(cacheDirPath: String) : OfflineSyncQueue {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val queueFilePath = "$cacheDirPath/$QUEUE_FILENAME"

    override suspend fun load(): List<SyncQueueEntry> = withContext(Dispatchers.Default) {
        mutex.withLock {
            val retained = OfflineSyncQueueRetention.retain(readInternal(), nowMillis())
            writeInternal(retained)
            retained
        }
    }

    override suspend fun enqueue(entry: SyncQueueEntry) = withContext(Dispatchers.Default) {
        mutex.withLock {
            val entries = readInternal().filterNot { it.id == entry.id } + entry
            writeInternal(OfflineSyncQueueRetention.retain(entries, nowMillis()))
        }
    }

    override suspend fun remove(ids: Set<String>) = withContext(Dispatchers.Default) {
        mutex.withLock { writeInternal(readInternal().filterNot { it.id in ids }) }
    }

    private fun readInternal(): List<SyncQueueEntry> {
        if (!NSFileManager.defaultManager.fileExistsAtPath(queueFilePath)) return emptyList()
        return runCatching {
            val content = NSString.stringWithContentsOfFile(
                path = queueFilePath,
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: return emptyList()
            json.decodeFromString<List<SyncQueueEntry>>(content)
        }.getOrDefault(emptyList())
    }

    private fun writeInternal(entries: List<SyncQueueEntry>) {
        val bytes = json.encodeToString(entries).encodeToByteArray()
        bytes.usePinned {
            val data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            NSFileManager.defaultManager.createFileAtPath(
                path = queueFilePath,
                contents = data,
                attributes = null,
            )
        }
    }

    private fun nowMillis(): Long = time(null) * 1_000

    private companion object {
        const val QUEUE_FILENAME = "offline_sync_queue.json"
    }
}
