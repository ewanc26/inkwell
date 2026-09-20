package uk.ewancroft.inkwell.data.remote

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okhttp3.ResponseBody.Companion.toResponseBody

class BoundedResponseBodyTest {
    @Test
    fun `reads a response within the byte budget`() {
        val body = "{\"ok\":true}".toByteArray().toResponseBody()

        assertEquals("{\"ok\":true}", body.readBoundedUtf8(maxBytes = 32))
    }

    @Test
    fun `rejects a response over the byte budget`() {
        val body = "12345".toByteArray().toResponseBody()

        assertFailsWith<IOException> { body.readBoundedUtf8(maxBytes = 4) }
    }
}
