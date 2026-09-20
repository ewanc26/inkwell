package uk.ewancroft.inkwell.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals

class Utf8OffsetsTest {
    @Test
    fun `exclusive byte end does not include following character`() {
        val text = "Read café at https://one.example"
        val start = text.indexOf("https://one.example")
        val end = start + "https://one.example".length
        val range = Utf8Offsets.byteRangeToCharRange(
            text,
            Utf8Offsets.charIndexToByteOffset(text, start),
            Utf8Offsets.charIndexToByteOffset(text, end),
        )

        assertEquals(start..(end - 1), range)
    }
}
