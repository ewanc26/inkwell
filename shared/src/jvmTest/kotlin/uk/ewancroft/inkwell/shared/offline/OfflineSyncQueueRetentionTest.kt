package uk.ewancroft.inkwell.shared.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Files

class OfflineSyncQueueRetentionTest {
    @Test
    fun `queue file has an explicit version and preserves entries`() {
        val entry = entry("versioned", 1)
        val decoded = Json.decodeFromString<SyncQueueFile>(
            Json.encodeToString(SyncQueueFile(entries = listOf(entry))),
        )

        assertEquals(1, decoded.version)
        assertEquals(listOf(entry), decoded.entries)
    }

    @Test
    fun `retention preserves stale mutations and keeps chronological order`() {
        val now = 30L * 24 * 60 * 60 * 1_000 + 1
        val retained = OfflineSyncQueueRetention.retain(
            entries = listOf(entry("stale", 0), entry("later", now - 1), entry("first", now - 2)),
            nowMillis = now,
        )

        assertEquals(listOf("stale", "first", "later"), retained.map(SyncQueueEntry::id))
    }

    @Test
    fun `retention replaces duplicate IDs with their latest value`() {
        val retained = OfflineSyncQueueRetention.retain(
            entries = listOf(entry("same", 1), entry("same", 2, kind = SyncMutationKind.Unrecommend)),
            nowMillis = 3,
        )

        assertEquals(1, retained.size)
        assertEquals(SyncMutationKind.Unrecommend, retained.single().kind)
    }

    @Test
    fun `legacy cache queue migrates into durable storage`() = runBlocking {
        val root = Files.createTempDirectory("inkwell-queue-migration").toFile()
        try {
            val durable = root.resolve("files")
            val legacy = root.resolve("cache").apply { mkdirs() }
            legacy.resolve("offline_sync_queue.json").writeText(
                Json.encodeToString(listOf(entry("legacy", 1)))
            )

            val queue = OfflineSyncQueueJvm(durable.path, legacy.path)
            assertEquals(listOf("legacy"), queue.load().map(SyncQueueEntry::id))
            assertEquals(true, durable.resolve("offline_sync_queue.json").exists())
            assertEquals(false, legacy.resolve("offline_sync_queue.json").exists())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `corrupt queue is surfaced and retained for recovery`() = runBlocking {
        val root = Files.createTempDirectory("inkwell-queue-corrupt").toFile()
        try {
            val durable = root.resolve("files").apply { mkdirs() }
            durable.resolve("offline_sync_queue.json").writeText("{truncated")

            val queue = OfflineSyncQueueJvm(durable.path, root.resolve("cache").path)
            assertFailsWith<Exception> { queue.load() }
            assertEquals(false, durable.resolve("offline_sync_queue.json").exists())
            assertEquals(true, durable.listFiles()?.any { it.name.startsWith("offline_sync_queue.json.corrupt-") } == true)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun entry(id: String, createdAt: Long, kind: SyncMutationKind = SyncMutationKind.Recommend) =
        SyncQueueEntry(
            id = id,
            accountDid = "did:example:me",
            kind = kind,
            subjectUri = "at://did:example/site.standard.document/post",
            createdAtMillis = createdAt,
        )
}
