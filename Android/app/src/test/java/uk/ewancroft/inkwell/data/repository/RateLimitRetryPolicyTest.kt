package uk.ewancroft.inkwell.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class RateLimitRetryPolicyTest {
    @Test fun `parses delta seconds and caps them`() {
        assertEquals(5_000L, RateLimitRetryPolicy.delayMillis("5", attempt = 0))
        assertEquals(60_000L, RateLimitRetryPolicy.delayMillis("999", attempt = 0))
        assertEquals(60_000L, RateLimitRetryPolicy.delayMillis("9223372036854775", attempt = 0))
    }

    @Test fun `parses HTTP dates against an injected clock`() {
        assertEquals(
            5_000L,
            RateLimitRetryPolicy.delayMillis(
                "Sun, 20 Sep 2026 00:00:05 GMT",
                attempt = 0,
                nowMillis = Instant.parse("2026-09-20T00:00:00Z").toEpochMilli()
            )
        )
    }

    @Test fun `falls back to bounded exponential delay for malformed headers`() {
        assertEquals(100L, RateLimitRetryPolicy.delayMillis("not-a-date", attempt = 0))
        assertEquals(400L, RateLimitRetryPolicy.delayMillis("not-a-date", attempt = 2))
        assertEquals(51_200L, RateLimitRetryPolicy.delayMillis("not-a-date", attempt = 20))
    }

    @Test fun `rejects negative and blank retry-after values`() {
        assertEquals(400L, RateLimitRetryPolicy.delayMillis("-1", attempt = 2))
        assertEquals(400L, RateLimitRetryPolicy.delayMillis("   ", attempt = 2))
    }

    @Test fun `expired HTTP date uses bounded exponential fallback`() {
        assertEquals(
            400L,
            RateLimitRetryPolicy.delayMillis(
                "Sun, 20 Sep 2026 00:00:05 GMT",
                attempt = 2,
                nowMillis = Instant.parse("2026-09-20T00:01:00Z").toEpochMilli()
            )
        )
    }
}
