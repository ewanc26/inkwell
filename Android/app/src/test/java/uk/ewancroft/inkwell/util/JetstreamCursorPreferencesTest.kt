package uk.ewancroft.inkwell.util

import kotlin.test.Test
import kotlin.test.assertEquals

class JetstreamCursorPreferencesTest {
    @Test
    fun cursorKeyIsStableAcrossSubscriptionOrdering() {
        assertEquals("did:a,did:b", JetstreamCursorPreferences.cursorKey(listOf("did:b", "did:a")))
    }
}
