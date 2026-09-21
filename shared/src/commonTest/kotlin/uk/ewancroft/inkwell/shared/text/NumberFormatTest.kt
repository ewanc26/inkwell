package uk.ewancroft.inkwell.shared.text

import kotlin.test.Test
import kotlin.test.assertEquals

class NumberFormatTest {
    @Test
    fun formatsSmallCountsWithoutAbbreviation() {
        assertEquals("42", NumberFormat.formatCount(42))
    }

    @Test
    fun abbreviatesThousandsAndMillions() {
        assertEquals("2K", NumberFormat.formatCount(2_300))
        assertEquals("1M", NumberFormat.formatCount(1_500_000))
    }
}
