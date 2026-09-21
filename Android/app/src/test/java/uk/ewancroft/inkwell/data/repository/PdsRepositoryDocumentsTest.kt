package uk.ewancroft.inkwell.data.repository

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonArray

class PdsRepositoryDocumentsTest {
    @Test
    fun `blob upload request preserves raw bytes and MIME type`() {
        val bytes = byteArrayOf(0, 1, -2, -1)
        val request = buildBlobUploadRequest(bytes, "image/png")

        assertContentEquals(bytes, request.body)
        assertEquals("image", request.contentType.contentType)
        assertEquals("png", request.contentType.contentSubtype)
    }

    @Test
    fun `paginates three pages and stops at the end cursor`() = kotlinx.coroutines.runBlocking {
        val pages = listOf(
            page("cursor-1", "one"),
            page("cursor-2", "two"),
            page(null, "three"),
        )
        var requestedCursors = emptyList<String?>()

        val records = paginateRecordPages { cursor ->
            requestedCursors = requestedCursors + cursor
            pages[requestedCursors.lastIndex]
        }

        assertEquals(listOf(null, "cursor-1", "cursor-2"), requestedCursors)
        assertEquals(listOf("at://one", "at://two", "at://three"), records.map { it.uri })
    }

    @Test
    fun `stops when a server repeats its cursor`() = kotlinx.coroutines.runBlocking {
        var requests = 0
        val records = paginateRecordPages { cursor ->
            requests += 1
            buildJsonObject {
                put("records", buildJsonArray {
                    add(buildJsonObject {
                        put("uri", "at://one")
                        put("value", buildJsonObject { put("title", "One") })
                    })
                })
                put("cursor", cursor ?: "same")
            }
        }

        assertEquals(2, requests)
        assertEquals(2, records.size)
    }

    private fun page(cursor: String?, uri: String) = buildJsonObject {
        put("records", buildJsonArray {
            add(buildJsonObject {
                put("uri", "at://$uri")
                put("value", buildJsonObject { put("title", uri) })
            })
        })
        cursor?.let { put("cursor", it) }
    }

    @Test
    fun `accepts an unknown or in-budget declared blob size`() {
        validateDeclaredBlobSize(null)
        validateDeclaredBlobSize(10L * 1024 * 1024)
    }

    @Test
    fun `reads a blob shorter than the reader limit without requiring a full buffer`() {
        val bytes = "valid blob".toByteArray()

        assertContentEquals(bytes, readBoundedBlob(bytes.toResponseBody()))
    }

    @Test
    fun `rejects a streamed blob that crosses the reader limit`() {
        val bytes = ByteArray(10 * 1024 * 1024 + 1)

        assertFailsWith<IOException> {
            readBoundedBlob(bytes.toResponseBody())
        }
    }

    @Test
    fun `rejects a declared blob size over the reader limit`() {
        assertFailsWith<IOException> {
            validateDeclaredBlobSize(10L * 1024 * 1024 + 1)
        }
    }

    @Test
    fun `rejects a negative declared blob size`() {
        assertFailsWith<IOException> {
            validateDeclaredBlobSize(-1)
        }
    }

    @Test
    fun `accepts matching response MIME type and rejects a mismatch`() {
        validateBlobContentType("application/json; charset=utf-8", "application/json")
        assertFailsWith<IOException> {
            validateBlobContentType("text/html", "application/json")
        }
    }

    @Test
    fun `rejects a missing response MIME type when one is expected`() {
        assertFailsWith<IOException> {
            validateBlobContentType(null, "image/png")
        }
    }

    @Test
    fun `delete input carries the expected record CID as swapRecord`() {
        val input = deleteRecordInput(
            repo = "did:plc:author",
            collection = "site.standard.document",
            rkey = "post",
            swapRecord = "bafyreighost",
        )

        assertEquals("did:plc:author", input["repo"]?.toString()?.trim('"'))
        assertEquals("site.standard.document", input["collection"]?.toString()?.trim('"'))
        assertEquals("post", input["rkey"]?.toString()?.trim('"'))
        assertEquals("bafyreighost", input["swapRecord"]?.toString()?.trim('"'))
    }

    @Test
    fun `update input carries record CID as swapRecord not swapCommit`() {
        val input = updateRecordInput(
            repo = "did:plc:author",
            collection = "site.standard.document",
            rkey = "post",
            record = buildJsonObject { put("title", "Updated") },
            recordCID = "bafyreighost",
        )

        assertEquals("did:plc:author", input["repo"]?.toString()?.trim('"'))
        assertEquals("site.standard.document", input["collection"]?.toString()?.trim('"'))
        assertEquals("post", input["rkey"]?.toString()?.trim('"'))
        assertEquals("bafyreighost", input["swapRecord"]?.toString()?.trim('"'))
        assertEquals(null, input["swapCommit"])
    }

    @Test
    fun `upload response validation accepts exact blob shape`() {
        val response = buildJsonObject {
            put("blob", buildJsonObject {
                put("mimeType", "image/png")
                put("size", 3)
                put("ref", buildJsonObject { put("\$link", "bafycid") })
            })
        }

        validateUploadBlobResponse(response, "image/png", 3)
    }

    @Test
    fun `upload response validation rejects mismatched metadata`() {
        val response = buildJsonObject {
            put("blob", buildJsonObject {
                put("mimeType", "image/jpeg")
                put("size", 3)
                put("ref", buildJsonObject { put("\$link", "bafycid") })
            })
        }

        assertFailsWith<IllegalStateException> {
            validateUploadBlobResponse(response, "image/png", 3)
        }
    }
}
