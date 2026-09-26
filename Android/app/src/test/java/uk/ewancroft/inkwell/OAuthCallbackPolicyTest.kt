package uk.ewancroft.inkwell

import android.net.Uri
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OAuthCallbackPolicyTest {
    @Test fun `accepts only the exact callback path`() {
        assertTrue(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback?code=abc&state=xyz")))
        assertTrue(OAuthCallbackPolicy.isCallback(Uri.parse("UK.EWANCROFT.INKWELL:/callback?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback-extra?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/other?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("other.app:/callback?code=abc")))
    }

    @Test fun `delivery fingerprints identify a redelivery without storing the code`() {
        val callback = Uri.parse("uk.ewancroft.inkwell:/callback?code=secret-code&state=secret-state")
        val fingerprint = OAuthCallbackPolicy.deliveryFingerprint(callback)

        // Same delivery, distinct Uri instance -> same fingerprint, so a warm
        // onNewIntent() redelivery or a post-rotation getIntent() is recognised.
        assertEquals(fingerprint, OAuthCallbackPolicy.deliveryFingerprint(Uri.parse(callback.toString())))
        // A genuinely different callback must not collide with it.
        assertNotEquals(
            fingerprint,
            OAuthCallbackPolicy.deliveryFingerprint(
                Uri.parse("uk.ewancroft.inkwell:/callback?code=other-code&state=secret-state"),
            ),
        )
        // The credential material never appears in the value that gets persisted.
        assertTrue(fingerprint!!.matches(Regex("[0-9a-f]{64}")))
        assertFalse(fingerprint.contains("secret-code"))
        assertFalse(fingerprint.contains("secret-state"))
    }

    @Test fun `non-callback URIs have no delivery fingerprint`() {
        assertNull(OAuthCallbackPolicy.deliveryFingerprint(Uri.parse("uk.ewancroft.inkwell:/other?code=abc")))
        assertNull(OAuthCallbackPolicy.deliveryFingerprint(Uri.parse("uk.ewancroft.inkwell:/callback-extra?code=abc")))
        assertNull(OAuthCallbackPolicy.deliveryFingerprint(Uri.parse("other.app:/callback?code=abc")))
        assertNull(
            OAuthCallbackPolicy.deliveryFingerprint(
                Uri.parse("inkwell://document?uri=at%3A%2F%2Fdid%3Aplc%3Ax%2Fsite.standard.document%2Fa"),
            ),
        )
    }

    @Test fun `content links accept only document AT URIs`() {
        val valid = Intent(Intent.ACTION_VIEW, Uri.parse(
            "inkwell://document?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        ))
        assertTrue(ContentDeepLinkPolicy.documentUri(valid)?.startsWith("at://") == true)

        val publication = Intent(Intent.ACTION_VIEW, Uri.parse(
            "inkwell://document?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.publication%2Fabc"
        ))
        assertTrue(ContentDeepLinkPolicy.documentUri(publication) == null)

        val mixedCase = Intent(Intent.ACTION_VIEW, Uri.parse(
            "INKWELL://DOCUMENT?uri=at%3A%2F%2Fdid%3Aplc%3Aexample%2Fsite.standard.document%2Fabc"
        ))
        assertTrue(ContentDeepLinkPolicy.documentUri(mixedCase)?.startsWith("at://") == true)
    }

    @Test fun `content links reject oauth and malformed inputs`() {
        assertTrue(ContentDeepLinkPolicy.documentUri(
            Intent(Intent.ACTION_VIEW, Uri.parse("uk.ewancroft.inkwell:/callback?uri=at%3A%2F%2Fdid%3Aplc%3Ax%2Fsite.standard.document%2Fa"))
        ) == null)
        assertTrue(ContentDeepLinkPolicy.documentUri(
            Intent(Intent.ACTION_VIEW, Uri.parse("inkwell://document?uri=not-an-at-uri"))
        ) == null)
    }
}
