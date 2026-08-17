package uk.ewancroft.inkwell.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AtUriTest {

    @Test
    fun parseValidDocumentUri() {
        val result = AtUri.parse("at://did:plc:alice/site.standard.document/3abc")
        assertNotNull(result)
        assertEquals("did:plc:alice", result.did)
        assertEquals("site.standard.document", result.collection)
        assertEquals("3abc", result.recordKey)
    }

    @Test
    fun parseValidPublicationUri() {
        val result = AtUri.parse("at://did:plc:alice/site.standard.publication/3pub")
        assertNotNull(result)
        assertEquals("did:plc:alice", result.did)
        assertEquals("site.standard.publication", result.collection)
        assertEquals("3pub", result.recordKey)
    }

    @Test
    fun parseRejectsHttpUrl() {
        assertNull(AtUri.parse("https://example.com/post"))
    }

    @Test
    fun parseRejectsMissingRecordKey() {
        assertNull(AtUri.parse("at://did:plc:alice/site.standard.document"))
    }

    @Test
    fun parseRejectsEmptySegments() {
        assertNull(AtUri.parse("at://did:plc:alice//3abc"))
        assertNull(AtUri.parse("at:///site.standard.document/3abc"))
    }

    @Test
    fun parseRejectsNonAtScheme() {
        assertNull(AtUri.parse("http://did:plc:alice/site.standard.document/3abc"))
    }

    @Test
    fun uriRoundTrip() {
        val original = "at://did:plc:alice/site.standard.document/3abc"
        val parsed = AtUri.parse(original)
        assertNotNull(parsed)
        assertEquals(original, parsed.uri)
    }

    @Test
    fun recordKeyWithSlashes() {
        val result = AtUri.parse("at://did:plc:alice/site.standard.document/a/b/c")
        assertNotNull(result)
        assertEquals("a/b/c", result.recordKey)
    }
}
