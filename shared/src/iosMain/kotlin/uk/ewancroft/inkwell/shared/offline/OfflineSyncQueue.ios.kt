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
class OfflineSyncQueueIos(durableDirPath: String, legacyCacheDirPath: String) : OfflineSyncQueue {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val queueFilePath = "$durableDirPath/$QUEUE_FILENAME"
    private val legacyQueueFilePath = "$legacyCacheDirPath/$QUEUE_FILENAME"

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
        migrateLegacyIfNeeded()
        if (!NSFileManager.defaultManager.fileExistsAtPath(queueFilePath)) return emptyList()
        return try {
            val content = NSString.stringWithContentsOfFile(
                path = queueFilePath,
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: error("Unable to read offline sync queue as UTF-8")
            runCatching { json.decodeFromString<SyncQueueFile>(content).entries }
                .getOrElse { json.decodeFromString<List<SyncQueueEntry>>(content) }
        } catch (error: Throwable) {
            preserveCorruptQueue()
            throw error
        }
    }

    private fun preserveCorruptQueue() {
        val preserved = "$queueFilePath.corrupt-${nowMillis()}"
        runCatching {
            NSFileManager.defaultManager.copyItemAtPath(
                srcPath = queueFilePath,
                toPath = preserved,
                error = null,
            )
        }
    }

    private fun writeInternal(entries: List<SyncQueueEntry>) {
        val bytes = json.encodeToString(SyncQueueFile(entries = entries)).encodeToByteArray()
        val temporaryPath = "$queueFilePath.tmp"
        bytes.usePinned {
            val data = NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
            NSFileManager.defaultManager.createFileAtPath(
                path = temporaryPath,
                contents = data,
                attributes = null,
            )
        }
        val replaced = NSFileManager.defaultManager.replaceItemAtURL(
            destinationURL = platform.Foundation.NSURL.fileURLWithPath(queueFilePath),
            withItemAtURL = platform.Foundation.NSURL.fileURLWithPath(temporaryPath),
            backupItemName = null,
            options = 0u,
            resultingItemURL = null,
            error = null,
        )
        check(replaced) { "Unable to replace offline sync queue" }
    }

    private fun migrateLegacyIfNeeded() {
        if (!NSFileManager.defaultManager.fileExistsAtPath(queueFilePath) &&
            NSFileManager.defaultManager.fileExistsAtPath(legacyQueueFilePath)
        ) {
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = queueFilePath.substringBeforeLast('/'),
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
            NSFileManager.defaultManager.moveItemAtPath(
                srcPath = legacyQueueFilePath,
                toPath = queueFilePath,
                error = null,
            )
        }
    }

    private fun nowMillis(): Long = time(null) * 1_000

    private companion object {
        const val QUEUE_FILENAME = "offline_sync_queue.json"
    }
}
