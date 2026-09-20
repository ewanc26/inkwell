package uk.ewancroft.inkwell.data.repository

import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdsResponseBodyReaderTest {
    @Test
    fun `accepts a body exactly at the safety limit`() {
        val body = "x".repeat(32)

        assertEquals(body, PdsResponseBodyReader.read(ByteArrayInputStream(body.toByteArray()), 32, 32))
    }

    @Test
    fun `rejects declared bodies above the safety limit before reading`() {
        assertThrows(IOException::class.java) {
            PdsResponseBodyReader.read(ByteArrayInputStream(ByteArray(1)), 33, 32)
        }
    }

    @Test
    fun `rejects unknown length streams that overrun the safety limit`() {
        assertThrows(IOException::class.java) {
            PdsResponseBodyReader.read(ByteArrayInputStream(ByteArray(33)), null, 32)
        }
    }
}
