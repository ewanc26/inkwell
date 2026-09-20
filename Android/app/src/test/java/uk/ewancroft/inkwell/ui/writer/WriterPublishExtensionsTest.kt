package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertDoesNotThrow
import org.junit.Assert.assertThrows
import org.junit.Test

class WriterPublishExtensionsTest {
    @Test
    fun `record at the conservative limit is accepted`() {
        val payload = "x".repeat(900 * 1024 - 128)
        assertDoesNotThrow { ensureDocumentRecordFits(buildJsonObject { put("content", payload) }) }
    }

    @Test
    fun `record above the conservative limit is rejected before submission`() {
        val payload = "x".repeat(900 * 1024)
        assertThrows(IllegalStateException::class.java) {
            ensureDocumentRecordFits(buildJsonObject { put("content", payload) })
        }
    }
}
