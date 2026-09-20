package uk.ewancroft.inkwell.shared.jetstream

import kotlin.test.Test
import kotlin.test.assertEquals

class JetstreamRetryPolicyTest {
    @Test
    fun delayIsBoundedAndExponential() {
        assertEquals(1_000L, JetstreamRetryPolicy.delayMillis(0))
        assertEquals(2_000L, JetstreamRetryPolicy.delayMillis(1))
        assertEquals(60_000L, JetstreamRetryPolicy.delayMillis(20))
    }
}
