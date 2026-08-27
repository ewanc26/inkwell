package uk.ewancroft.inkwell.shared.offline

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OfflineContentCacheRetentionTest {
    @Test
    fun `retention prunes least recently accessed full records`() {
        val policy = OfflineCachePolicy(maxEntries = 2, maxBytes = 1_000, metadataTtlMillis = 1_000)
        val retained = OfflineContentCacheRetention.retain(
            records = listOf(
                record(uri = "at://did:example:one/site.standard.document/one", accessedAt = 1),
                record(uri = "at://did:example:two/site.standard.document/two", accessedAt = 2),
                record(uri = "at://did:example:three/site.standard.document/three", accessedAt = 3),
            ),
            policy = policy,
            nowMillis = 4,
        )

        assertEquals(
            listOf(
                "at://did:example:two/site.standard.document/two",
                "at://did:example:three/site.standard.document/three",
            ),
            retained.map(CachedOfflineRecord::uri),
        )
    }

    @Test
    fun `retention keeps the latest version of a record URI`() {
        val uri = "at://did:example:one/site.standard.document/one"
        val retained = OfflineContentCacheRetention.retain(
            records = listOf(
                record(uri = uri, payload = "{\"version\":1}", accessedAt = 1),
                record(uri = uri, payload = "{\"version\":2}", accessedAt = 2),
            ),
            policy = OfflineCachePolicy(),
            nowMillis = 3,
        )

        assertEquals(1, retained.size)
        assertEquals("{\"version\":2}", retained.single().recordJson)
    }

    @Test
    fun `offline record cache rejects image payloads`() {
        assertFailsWith<IllegalArgumentException> {
            CachedOfflineRecord(
                uri = "at://did:example:one/site.standard.document/one",
                kind = OfflineCacheKind.Image,
                authorDid = "did:example:one",
                recordJson = "{}",
                cachedAtMillis = 1,
            )
        }
    }

    private fun record(
        uri: String,
        payload: String = "{\"title\":\"Cached\"}",
        accessedAt: Long,
    ) = CachedOfflineRecord(
        uri = uri,
        kind = OfflineCacheKind.Document,
        authorDid = "did:example:author",
        recordJson = payload,
        cachedAtMillis = 1,
        lastAccessedAtMillis = accessedAt,
    )
}
