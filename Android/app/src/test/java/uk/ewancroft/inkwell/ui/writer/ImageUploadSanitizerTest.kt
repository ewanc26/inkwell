package uk.ewancroft.inkwell.ui.writer

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ImageUploadSanitizerTest {
    @Test
    fun `rejects oversized input before image decoding`() {
        assertFailsWith<IllegalArgumentException> {
            ImageUploadSanitizer.sanitize(ByteArray(10 * 1024 * 1024 + 1))
        }
    }
}
