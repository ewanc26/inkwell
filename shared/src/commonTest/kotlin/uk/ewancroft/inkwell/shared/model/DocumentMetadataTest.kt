package uk.ewancroft.inkwell.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DocumentMetadataTest {

    private fun record(vararg pairs: Pair<String, Any?>): Map<String, Any?> = mapOf(
        "\$type" to "site.standard.document",
        "site" to "at://did:plc:example/site.standard.publication/self",
        "title" to "A document",
        "publishedAt" to "2026-01-01T00:00:00Z",
        *pairs,
    )

    // ── Reading ────────────────────────────────────────────────────────────

    @Test
    fun `a record with no metadata reads as empty rather than failing`() {
        val metadata = DocumentMetadata.read(record())

        assertEquals(emptyList(), metadata.tags)
        assertEquals(emptyList(), metadata.contributors)
        assertEquals(emptyList(), metadata.labels)
        assertEquals(emptyList(), metadata.links)
        assertNull(metadata.coverImage)
        assertNull(metadata.bskyPostRef)
        assertNull(metadata.updatedAt)
        assertEquals("2026-01-01T00:00:00Z", metadata.publishedAt)
    }

    @Test
    fun `tags are trimmed and blank entries dropped`() {
        val metadata = DocumentMetadata.read(record("tags" to listOf(" atproto ", "", "  ", "kotlin")))

        assertEquals(listOf("atproto", "kotlin"), metadata.tags)
    }

    @Test
    fun `contributors read did role and displayName`() {
        val metadata = DocumentMetadata.read(
            record(
                "contributors" to listOf(
                    mapOf(
                        "\$type" to DocumentMetadata.CONTRIBUTOR_TYPE,
                        "did" to "did:plc:editor",
                        "role" to "editor",
                        "displayName" to "The Editor",
                    ),
                    mapOf("did" to "did:plc:bare"),
                ),
            ),
        )

        assertEquals(
            listOf(
                DocumentMetadata.Contributor("did:plc:editor", "editor", "The Editor"),
                DocumentMetadata.Contributor("did:plc:bare"),
            ),
            metadata.contributors,
        )
    }

    @Test
    fun `a malformed contributor does not take its valid siblings down`() {
        val metadata = DocumentMetadata.read(
            record(
                "contributors" to listOf(
                    "not an object",
                    mapOf("role" to "editor"),
                    mapOf("did" to "did:plc:valid"),
                ),
                "tags" to listOf("still-here"),
            ),
        )

        assertEquals(listOf(DocumentMetadata.Contributor("did:plc:valid")), metadata.contributors)
        assertEquals(listOf("still-here"), metadata.tags)
    }

    @Test
    fun `labels read the selfLabels values array`() {
        val metadata = DocumentMetadata.read(
            record(
                "labels" to mapOf(
                    "\$type" to DocumentMetadata.SELF_LABELS_TYPE,
                    "values" to listOf(
                        mapOf("val" to "graphic-media"),
                        mapOf("val" to "graphic-media"),
                        mapOf("noVal" to true),
                    ),
                ),
            ),
        )

        assertEquals(listOf("graphic-media"), metadata.labels)
    }

    @Test
    fun `bskyPostRef reads uri and optional cid`() {
        val withCid = DocumentMetadata.read(
            record("bskyPostRef" to mapOf("uri" to "at://did:plc:a/app.bsky.feed.post/1", "cid" to "bafy")),
        )
        assertEquals(DocumentMetadata.BskyPostRef("at://did:plc:a/app.bsky.feed.post/1", "bafy"), withCid.bskyPostRef)

        val withoutUri = DocumentMetadata.read(record("bskyPostRef" to mapOf("cid" to "bafy")))
        assertNull(withoutUri.bskyPostRef)
    }

    @Test
    fun `an unknown link variant keeps its raw shape and reads as opaque`() {
        val raw = mapOf<String, Any?>("\$type" to "site.standard.document#somethingNew", "payload" to mapOf("a" to 1))
        val metadata = DocumentMetadata.read(record("links" to listOf(raw)))

        assertEquals(1, metadata.links.size)
        assertEquals(raw, metadata.links.single().raw)
        assertEquals("site.standard.document#somethingNew", metadata.links.single().type)
        assertTrue(metadata.links.single().isOpaque)
    }

    @Test
    fun `a link variant exposing a uri and title is presentable`() {
        val metadata = DocumentMetadata.read(
            record("links" to listOf(mapOf("\$type" to "x#link", "uri" to "https://example.com", "title" to "Example"))),
        )

        val link = metadata.links.single()
        assertEquals("https://example.com", link.displayUri)
        assertEquals("Example", link.title)
        assertFalse(link.isOpaque)
    }

    @Test
    fun `links accepts a single union object as well as an array`() {
        val metadata = DocumentMetadata.read(record("links" to mapOf("url" to "https://example.com")))

        assertEquals("https://example.com", metadata.links.single().displayUri)
    }

    // ── Writing ────────────────────────────────────────────────────────────

    @Test
    fun `applying metadata leaves unmodelled record fields untouched`() {
        val existing = record(
            "theme" to mapOf("backgroundColor" to "#fff"),
            "somethingInkwellDoesNotModel" to listOf(1, 2, 3),
        )

        val result = DocumentMetadata.applyTo(existing, DocumentMetadata(tags = listOf("a")))

        assertEquals(mapOf("backgroundColor" to "#fff"), result["theme"])
        assertEquals(listOf(1, 2, 3), result["somethingInkwellDoesNotModel"])
        assertEquals("A document", result["title"])
    }

    @Test
    fun `empty metadata removes its keys rather than writing empty containers`() {
        val existing = record(
            "tags" to listOf("old"),
            "contributors" to listOf(mapOf("did" to "did:plc:old")),
            "labels" to mapOf("values" to listOf(mapOf("val" to "old"))),
            "links" to listOf(mapOf("uri" to "https://old.example")),
        )

        val result = DocumentMetadata.applyTo(existing, DocumentMetadata())

        assertFalse(result.containsKey("tags"))
        assertFalse(result.containsKey("contributors"))
        assertFalse(result.containsKey("labels"))
        assertFalse(result.containsKey("links"))
    }

    @Test
    fun `contributors and labels are written in their canonical wire shapes`() {
        val result = DocumentMetadata.applyTo(
            record(),
            DocumentMetadata(
                contributors = listOf(DocumentMetadata.Contributor(" did:plc:editor ", " editor ", null)),
                labels = listOf("graphic-media", "graphic-media", " "),
            ),
        )

        assertEquals(
            listOf(
                mapOf(
                    "\$type" to DocumentMetadata.CONTRIBUTOR_TYPE,
                    "did" to "did:plc:editor",
                    "role" to "editor",
                ),
            ),
            result["contributors"],
        )
        assertEquals(
            mapOf(
                "\$type" to DocumentMetadata.SELF_LABELS_TYPE,
                "values" to listOf(mapOf("\$type" to DocumentMetadata.SELF_LABEL_TYPE, "val" to "graphic-media")),
            ),
            result["labels"],
        )
    }

    @Test
    fun `tags are deduplicated and trimmed on write`() {
        val result = DocumentMetadata.applyTo(record(), DocumentMetadata(tags = listOf(" a ", "a", "b", "")))

        assertEquals(listOf("a", "b"), result["tags"])
    }

    @Test
    fun `a contributor without a did is not written`() {
        val result = DocumentMetadata.applyTo(
            record(),
            DocumentMetadata(contributors = listOf(DocumentMetadata.Contributor(did = "   "))),
        )

        assertFalse(result.containsKey("contributors"))
    }

    @Test
    fun `unknown link variants round-trip through a read and a write`() {
        val raw = mapOf<String, Any?>(
            "\$type" to "site.standard.document#futureVariant",
            "nested" to mapOf("keep" to listOf("me")),
        )
        val original = record("links" to listOf(raw))

        val result = DocumentMetadata.applyTo(original, DocumentMetadata.read(original))

        assertEquals(listOf(raw), result["links"])
    }

    @Test
    fun `cover image blob is written back verbatim`() {
        val blob = mapOf<String, Any?>(
            "\$type" to "blob",
            "ref" to mapOf("\$link" to "bafyreicover"),
            "mimeType" to "image/jpeg",
            "size" to 1234,
        )

        val result = DocumentMetadata.applyTo(record(), DocumentMetadata(coverImage = blob))

        assertEquals(blob, result["coverImage"])
    }
}
