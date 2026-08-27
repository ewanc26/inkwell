package uk.ewancroft.inkwell.shared.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import uk.ewancroft.inkwell.shared.offline.OfflineCachePolicy

class FeedCacheRetentionTest {
    @Test
    fun `retention removes the oldest feed entry when over capacity`() {
        val retained = FeedCacheRetention.retain(
            items = listOf(item("one", 1), item("two", 2), item("three", 3)),
            nowMillis = 4,
            policy = OfflineCachePolicy(maxEntries = 2, maxBytes = 10_000, metadataTtlMillis = 10_000),
        )

        assertEquals(
            listOf("at://did:example/two", "at://did:example/three"),
            retained.map(CachedFeedItem::uri),
        )
    }

    @Test
    fun `retention keeps the newest cached version of a URI`() {
        val retained = FeedCacheRetention.retain(
            items = listOf(item("same", 1, title = "old"), item("same", 2, title = "new")),
            nowMillis = 3,
        )

        assertEquals(1, retained.size)
        assertEquals("new", retained.single().title)
    }

    private fun item(uri: String, cachedAt: Long, title: String = uri) = CachedFeedItem(
        uri = "at://did:example/$uri",
        authorDID = "did:example",
        site = "at://did:example/site.standard.publication/site",
        title = title,
        publishedAt = "2026-08-27T00:00:00Z",
        cachedAt = cachedAt,
    )
}
