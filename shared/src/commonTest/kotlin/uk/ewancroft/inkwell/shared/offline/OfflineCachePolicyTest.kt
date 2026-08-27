package uk.ewancroft.inkwell.shared.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OfflineCachePolicyTest {
    @Test
    fun `expired entries are evicted before capacity entries`() {
        val policy = OfflineCachePolicy(maxEntries = 2, maxBytes = 300, metadataTtlMillis = 10, imageTtlMillis = 5)
        val entries = listOf(
            entry("expired", OfflineCacheKind.Document, cachedAt = 0, accessedAt = 9, bytes = 50),
            entry("oldest", OfflineCacheKind.Document, cachedAt = 9, accessedAt = 10, bytes = 200),
            entry("newest", OfflineCacheKind.Document, cachedAt = 9, accessedAt = 11, bytes = 200),
        )

        assertEquals(setOf("expired", "oldest"), policy.keysToEvict(entries, nowMillis = 10))
    }

    @Test
    fun `image entries use the shorter image retention period`() {
        val policy = OfflineCachePolicy(metadataTtlMillis = 100, imageTtlMillis = 10)

        assertEquals(true, policy.isExpired(entry("image", OfflineCacheKind.Image, 0, 0, 1), 10))
        assertEquals(false, policy.isExpired(entry("document", OfflineCacheKind.Document, 0, 0, 1), 10))
    }

    @Test
    fun `cache policy rejects invalid limits`() {
        assertFailsWith<IllegalArgumentException> { OfflineCachePolicy(maxEntries = 0) }
        assertFailsWith<IllegalArgumentException> { OfflineCachePolicy(maxBytes = 0) }
    }

    @Test
    fun `queued comments require text while other actions reject it`() {
        assertFailsWith<IllegalArgumentException> {
            SyncQueueEntry(
                id = "one",
                accountDid = "did:example:me",
                kind = SyncMutationKind.CreateComment,
                subjectUri = "at://did:example:me/post/1",
                createdAtMillis = 1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            SyncQueueEntry(
                id = "two",
                accountDid = "did:example:me",
                kind = SyncMutationKind.Recommend,
                subjectUri = "at://did:example:me/post/1",
                createdAtMillis = 1,
                commentText = "No",
            )
        }
    }

    @Test
    fun `queued mutations require an owning account`() {
        assertFailsWith<IllegalArgumentException> {
            SyncQueueEntry(
                id = "one",
                accountDid = "",
                kind = SyncMutationKind.Recommend,
                subjectUri = "at://did:example:me/site.standard.document/post",
                createdAtMillis = 1,
            )
        }
    }

    private fun entry(
        key: String,
        kind: OfflineCacheKind,
        cachedAt: Long,
        accessedAt: Long,
        bytes: Long,
    ) = OfflineCacheEntry(key, kind, cachedAt, accessedAt, bytes)
}
