package uk.ewancroft.inkwell.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalizedNumberFormatTest {
    @Test
    fun formatsGroupingForTheActiveLocale() {
        assertEquals("1.234", formatLocalizedInteger(1234, NumberFormat.getIntegerInstance(Locale.GERMANY)))
    }

    @Test
    fun formatsPercentageForTheActiveLocale() {
        assertEquals("42%", formatLocalizedPercentage(0.42f, NumberFormat.getPercentInstance(Locale.US)))
        assertEquals("%42", formatLocalizedPercentage(0.42f, NumberFormat.getPercentInstance(Locale.forLanguageTag("tr-TR"))))
    }
}
