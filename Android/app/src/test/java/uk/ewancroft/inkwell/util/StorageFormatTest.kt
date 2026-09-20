package uk.ewancroft.inkwell.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class StorageFormatTest {
    @Test
    fun usesLocaleDecimalSeparator() {
        val format = NumberFormat.getNumberInstance(Locale.GERMANY)

        assertEquals("1,5 MB", formatCacheSize((1.5 * 1024 * 1024).toLong(), format))
    }

    @Test
    fun keepsTinyCachesAsEmpty() {
        val format = NumberFormat.getNumberInstance(Locale.US)

        assertEquals("Empty", formatCacheSize(0, format))
    }
}
