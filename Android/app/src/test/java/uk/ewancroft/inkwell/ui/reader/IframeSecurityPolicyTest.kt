package uk.ewancroft.inkwell.ui.reader

import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IframeSecurityPolicyTest {
    @Test
    fun `only https hosts can be loaded`() {
        assertTrue(IframeSecurityPolicy.isAllowedInitial(Uri.parse("https://embed.example/video")))
        assertFalse(IframeSecurityPolicy.isAllowedInitial(Uri.parse("http://embed.example/video")))
        assertFalse(IframeSecurityPolicy.isAllowedInitial(Uri.parse("javascript:alert(1)")))
    }

    @Test
    fun `navigation stays on the original https host`() {
        assertTrue(
            IframeSecurityPolicy.isAllowedNavigation(
                "embed.example",
                Uri.parse("https://embed.example/redirected")
            )
        )
        assertFalse(
            IframeSecurityPolicy.isAllowedNavigation(
                "embed.example",
                Uri.parse("https://attacker.example/steal")
            )
        )
        assertFalse(
            IframeSecurityPolicy.isAllowedNavigation(
                "embed.example",
                Uri.parse("custom://embed.example/action")
            )
        )
    }
}
