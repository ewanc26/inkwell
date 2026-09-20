package uk.ewancroft.inkwell.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContentLinkPolicyTest {
    @Test fun `allows web links case insensitively`() {
        assertEquals("https", ContentLinkPolicy.parse("HTTPS://example.com/a")?.scheme)
    }

    @Test fun `allows explicit user action schemes`() {
        assertEquals("mailto", ContentLinkPolicy.parse("mailto:person@example.com")?.scheme)
        assertEquals("tel", ContentLinkPolicy.parse("tel:+441234567890")?.scheme)
    }

    @Test fun `rejects dangerous and application schemes`() {
        listOf("javascript:alert(1)", "data:text/html,hi", "file:///tmp/a", "content://x/a",
            "intent://example.com", "uk.ewancroft.inkwell:/callback?code=secret").forEach {
            assertNull(it, ContentLinkPolicy.parse(it))
        }
    }

    @Test fun `rejects whitespace control characters and malformed web urls`() {
        assertNull(ContentLinkPolicy.parse(" https://example.com"))
        assertNull(ContentLinkPolicy.parse("https://example.com\n"))
        assertNull(ContentLinkPolicy.parse("https:///missing-host"))
        assertNull(ContentLinkPolicy.parse("https://example.com\u0000"))
    }
}
