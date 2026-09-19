package uk.ewancroft.inkwell

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OAuthCallbackPolicyTest {
    @Test fun `accepts only the exact callback path`() {
        assertTrue(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback?code=abc&state=xyz")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/callback-extra?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("uk.ewancroft.inkwell:/other?code=abc")))
        assertFalse(OAuthCallbackPolicy.isCallback(Uri.parse("other.app:/callback?code=abc")))
    }
}
