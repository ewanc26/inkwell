package uk.ewancroft.inkwell.data.repository

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import okhttp3.ResponseBody.Companion.toResponseBody

class PdsRepositoryDocumentsTest {
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
}
