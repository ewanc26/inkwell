package uk.ewancroft.inkwell.ui.reader

import kotlin.test.Test
import kotlin.test.assertNull

class PostDetailContentParsingTest {
    @Test
    fun `blob-backed pages reject excessive JSON nesting before decoding`() {
        val nested = buildString {
            repeat(33) { append("[") }
            append("null")
            repeat(33) { append("]") }
        }

        assertNull(decodeSafeLeafletPages(nested))
    }
}
