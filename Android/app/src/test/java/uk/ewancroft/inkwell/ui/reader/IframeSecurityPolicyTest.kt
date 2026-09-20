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
        val origin = Uri.parse("https://embed.example/frame")
        assertTrue(
            IframeSecurityPolicy.isAllowedNavigation(
                origin,
                Uri.parse("https://embed.example/redirected")
            )
        )
        assertFalse(
            IframeSecurityPolicy.isAllowedNavigation(
                origin,
                Uri.parse("https://attacker.example/steal")
            )
        )
        assertFalse(
            IframeSecurityPolicy.isAllowedNavigation(
                origin,
                Uri.parse("custom://embed.example/action")
            )
        )
        assertFalse(
            IframeSecurityPolicy.isAllowedNavigation(
                Uri.parse("https://embed.example:8443/frame"),
                Uri.parse("https://embed.example/redirected")
            )
        )
    }
}
