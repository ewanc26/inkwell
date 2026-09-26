package uk.ewancroft.inkwell.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.ewancroft.inkwell.shared.oauth.InkwellOAuthScopes
import uk.ewancroft.inkwell.shared.oauth.OAuthScopes
import uk.ewancroft.inkwell.shared.oauth.StandardSitePermissionSets

class OAuthModuleTest {

    @Test
    fun `scope tokens remain separated and unique`() {
        val tokens = OAuthModule.SCOPE.split(' ')

        assertEquals(tokens.size, tokens.toSet().size)
        assertTrue(tokens.all { it.isNotBlank() })
        assertTrue(OAuthScopes.REPO_USER in tokens)
        assertTrue("repo:app.bsky.graph.block?action=create&action=delete" in tokens)
    }

    @Test
    fun `scope string is the shared runtime scope string`() {
        assertEquals(InkwellOAuthScopes.runtimeScopeString(), OAuthModule.SCOPE)
    }

    @Test
    fun `android requests the granular Standard site scopes while the permission set is disabled`() {
        val tokens = OAuthModule.SCOPE.split(' ')

        assertFalse(StandardSitePermissionSets.USE_PERMISSION_SET)
        assertTrue(tokens.none { it.startsWith(StandardSitePermissionSets.INCLUDE_PREFIX) })
        assertTrue(StandardSitePermissionSets.AUTH_FULL_EXPANSION.all { it in tokens })
    }

    @Test
    fun `scope string never exceeds what client metadata declares`() {
        val declared = InkwellOAuthScopes.clientMetadataScopes().toSet()

        for (scope in OAuthModule.SCOPE.split(' ')) {
            assertTrue("undeclared runtime scope: $scope", scope in declared)
        }
    }
}
