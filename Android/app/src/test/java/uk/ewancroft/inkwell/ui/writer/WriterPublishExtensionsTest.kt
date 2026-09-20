package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WriterPublishExtensionsTest {
    @Test
    fun `accepts a record below the serialized size limit`() {
        ensureDocumentRecordFits(buildJsonObject {
            put("textContent", "x".repeat(100))
        })
    }

    @Test
    fun `rejects a record over the serialized size limit`() {
        assertFailsWith<IllegalStateException> {
            ensureDocumentRecordFits(buildJsonObject {
                put("textContent", "x".repeat(900 * 1024))
            })
        }
    }
}
