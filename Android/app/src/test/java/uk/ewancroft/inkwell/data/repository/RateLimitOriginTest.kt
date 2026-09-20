package uk.ewancroft.inkwell.data.repository

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class RateLimitOriginTest {
    @Test
    fun `omits default HTTPS port`() {
        assertEquals("https://pds.example", rateLimitOrigin("https://pds.example:443/xrpc/test".toHttpUrl()))
    }

    @Test
    fun `preserves non-default port`() {
        assertEquals("https://pds.example:8443", rateLimitOrigin("https://pds.example:8443/xrpc/test".toHttpUrl()))
    }
}
