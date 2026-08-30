package uk.ewancroft.inkwell.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.ewancroft.inkwell.shared.oauth.OAuthScopes

class OAuthModuleTest {

    @Test
    fun `scope tokens remain separated and unique`() {
        val tokens = OAuthModule.SCOPE.split(' ')

        assertEquals(tokens.size, tokens.toSet().size)
        assertTrue(tokens.all { it.isNotBlank() })
        assertTrue(OAuthScopes.REPO_USER in tokens)
        assertTrue("repo:app.bsky.graph.block?action=create&action=delete" in tokens)
    }
}
