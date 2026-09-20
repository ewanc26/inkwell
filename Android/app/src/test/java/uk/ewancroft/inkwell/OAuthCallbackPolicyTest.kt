package uk.ewancroft.inkwell

import android.net.Uri
import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OAuthCallbackPolicyTest {
    @Test fun `accepts only the exact callback path`() {
        assertTrue(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback?code=abc&state=xyz")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback-extra?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/other?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("other.app:/callback?code=abc")))
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
