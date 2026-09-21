package uk.ewancroft.inkwell.ui.writer

import org.junit.Assert.assertEquals
import org.junit.Test

class WriterImageAltTextTest {
    @Test
    fun `meaningful alt text is preserved and surrounding whitespace is removed`() {
        assertEquals(
            "\n![A mountain at sunrise](bafyimage)\n",
            markdownImageReference("  A mountain at sunrise  ", "bafyimage"),
        )
    }

    @Test
    fun `decorative images use an explicitly empty alt attribute`() {
        assertEquals(
            "\n![](bafyimage)\n",
            markdownImageReference("", "bafyimage"),
        )
    }
}
