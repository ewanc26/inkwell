package uk.ewancroft.inkwell.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [listRecordsUrl] and [getRecordUrl] build XRPC queries with
 * `HttpUrl.Builder` instead of hand-rolled string interpolation. These
 * tests confirm special-character values round-trip through the encoder
 * unchanged, matching the behaviour [ConstellationClient] already relies on.
 */
class PdsRepositoryQueryUrlTest {

    @Test
    fun `listRecordsUrl encodes an ampersand in the collection value`() {
        val url = listRecordsUrl(
            baseUrl = "https://pds.example",
            did = "did:plc:abc123",
            collection = "site.standard&evil=1",
            limit = 25,
            cursor = null,
        )

        assertEquals("did:plc:abc123", url.queryParameter("repo"))
        assertEquals("site.standard&evil=1", url.queryParameter("collection"))
        assertEquals("25", url.queryParameter("limit"))
        assertNull(url.queryParameter("cursor"))
        // The injected `&evil=1` must not become a second top-level query
        // parameter — it must stay inside the encoded `collection` value.
        assertEquals(emptyList<String?>(), url.queryParameterValues("evil"))
        assertEquals(setOf("repo", "collection", "limit"), url.queryParameterNames)
    }

    @Test
    fun `listRecordsUrl round-trips question marks, hashes, spaces and unicode in the cursor`() {
        val cursor = "weird?#value with space and ünïcödé 漢字"
        val url = listRecordsUrl(
            baseUrl = "https://pds.example",
            did = "did:plc:abc123",
            collection = "site.standard.document",
            limit = 10,
            cursor = cursor,
        )

        assertEquals(cursor, url.queryParameter("cursor"))
    }

    @Test
    fun `listRecordsUrl builds the expected path and endpoint`() {
        val url = listRecordsUrl(
            baseUrl = "https://pds.example",
            did = "did:plc:abc123",
            collection = "site.standard.document",
            limit = 25,
            cursor = "next-page",
        )

        assertEquals("https", url.scheme)
        assertEquals("pds.example", url.host)
        assertEquals("/xrpc/com.atproto.repo.listRecords", url.encodedPath)
        assertEquals("next-page", url.queryParameter("cursor"))
    }

    @Test
    fun `getRecordUrl encodes a hash and slash in the rkey`() {
        val url = getRecordUrl(
            baseUrl = "https://pds.example",
            did = "did:plc:abc123",
            collection = "site.standard.document",
            rkey = "weird#rkey/with slash",
        )

        assertEquals("did:plc:abc123", url.queryParameter("repo"))
        assertEquals("site.standard.document", url.queryParameter("collection"))
        assertEquals("weird#rkey/with slash", url.queryParameter("rkey"))
        assertEquals("/xrpc/com.atproto.repo.getRecord", url.encodedPath)
    }

    @Test
    fun `getRecordUrl round-trips a did containing a colon and percent-like sequence`() {
        val did = "did:web:example.com:user:100%owned"
        val url = getRecordUrl(
            baseUrl = "https://pds.example",
            did = did,
            collection = "site.standard.publication",
            rkey = "self",
        )

        assertEquals(did, url.queryParameter("repo"))
    }
}
