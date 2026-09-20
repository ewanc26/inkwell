package uk.ewancroft.inkwell.util

import kotlin.test.Test
import kotlin.test.assertEquals

class BidiTextTest {
    @Test
    fun wrapsStrongRtlTextForAnEnglishLabel() {
        val isolated = "שלום.example".bidiIsolated()

        assertEquals("\u2068שלום.example\u2069", isolated)
    }
}
