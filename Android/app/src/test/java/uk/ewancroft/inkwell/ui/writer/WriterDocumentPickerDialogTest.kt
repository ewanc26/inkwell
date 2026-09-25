package uk.ewancroft.inkwell.ui.writer

import java.io.IOException
import kotlin.test.assertFailsWith
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WriterDocumentPickerDialogTest {

    @Test
    fun `writerDocumentListUrl encodes an ampersand-bearing did into a single repo parameter`() {
        val did = "did:plc:abc&collection=evil&limit=999"
        val url = writerDocumentListUrl(did)

        assertEquals(did, url.queryParameter("repo"))
        assertEquals("site.standard.document", url.queryParameter("collection"))
        assertEquals("25", url.queryParameter("limit"))
        assertEquals(setOf("repo", "collection", "limit"), url.queryParameterNames)
    }

    @Test
    fun `writerDocumentListUrl round-trips unicode and whitespace in the did`() {
        val did = "did:web:example.com:ünïcödé user"
        val url = writerDocumentListUrl(did)

        assertEquals(did, url.queryParameter("repo"))
    }

    @Test
    fun `parseWriterDocumentListResponse rejects a non-2xx response instead of parsing the body as JSON`() {
        val exception = assertFailsWith<IOException> {
            parseWriterDocumentListResponse(
                isSuccessful = false,
                code = 500,
                body = "<html>not json</html>",
            )
        }
        assertTrue(exception.message.orEmpty().contains("500"))
    }

    @Test
    fun `parseWriterDocumentListResponse rejects a successful response with a missing body`() {
        assertFailsWith<IOException> {
            parseWriterDocumentListResponse(isSuccessful = true, code = 200, body = null)
        }
    }

    @Test
    fun `parseWriterDocumentListResponse parses records from a successful response`() {
        val body = """
            {"records":[
                {"uri":"at://did:plc:abc/site.standard.document/one","value":{"title":"First"}},
                {"uri":"at://did:plc:abc/site.standard.document/two","value":{}}
            ]}
        """.trimIndent()

        val documents = parseWriterDocumentListResponse(isSuccessful = true, code = 200, body = body)

        assertEquals(2, documents.size)
        assertEquals("First", documents[0].title)
        assertEquals("Untitled", documents[1].title)
    }
}
