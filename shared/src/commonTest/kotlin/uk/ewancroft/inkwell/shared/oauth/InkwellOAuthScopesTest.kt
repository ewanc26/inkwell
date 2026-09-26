package uk.ewancroft.inkwell.shared.oauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the OAuth scope request. Every assertion here protects live authentication for a
 * shipping app: a wrong scope string does not degrade a feature, it stops anyone logging
 * in.
 */
class InkwellOAuthScopesTest {

    /**
     * The exact scope string Inkwell has been shipping. Refactoring scope construction into
     * shared KMP must not change a single byte of what goes on the wire.
     */
    private val shippedScopeString =
        "atproto blob:*/* " +
            "repo:site.standard.publication repo:site.standard.document " +
            "repo:site.standard.graph.subscription repo:site.standard.graph.recommend " +
            "repo:pub.leaflet.comment repo:app.userinput.discussion " +
            "repo:uk.ewancroft.inkwell.user " +
            "repo:app.bsky.graph.block?action=create&action=delete " +
            "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview " +
            "rpc:com.atproto.moderation.createReport?aud=did:web:api.bsky.app%23bsky_appview"

    @Test
    fun granularRequestMatchesTheShippedScopeStringByteForByte() {
        assertEquals(shippedScopeString, InkwellOAuthScopes.runtimeScopeString(usePermissionSet = false))
    }

    @Test
    fun permissionSetIsOffByDefaultSoTheLiveRequestIsUnchanged() {
        // Flipping USE_PERMISSION_SET is a deliberate act that must also update this test.
        assertFalse(StandardSitePermissionSets.USE_PERMISSION_SET)
        assertEquals(shippedScopeString, InkwellOAuthScopes.runtimeScopeString())
    }

    @Test
    fun permissionSetRequestReplacesExactlyTheFourStandardSiteScopes() {
        val withSet = InkwellOAuthScopes.runtimeScopes(usePermissionSet = true)
        val granular = InkwellOAuthScopes.runtimeScopes(usePermissionSet = false)

        assertTrue(StandardSitePermissionSets.INCLUDE_AUTH_FULL in withSet)
        assertTrue(withSet.none { it.startsWith("repo:site.standard.") })
        assertEquals(
            granular - StandardSitePermissionSets.AUTH_FULL_EXPANSION.toSet(),
            withSet - StandardSitePermissionSets.INCLUDE_AUTH_FULL,
        )
    }

    @Test
    fun everyRuntimeScopeIsALiteralStringMemberOfDeclaredMetadata() {
        // The Authorization Server compares requested scopes to the client metadata
        // `scope` field by plain string membership; "equivalent" is not good enough.
        val declared = InkwellOAuthScopes.clientMetadataScopes()
        for (usePermissionSet in listOf(false, true)) {
            for (scope in InkwellOAuthScopes.runtimeScopes(usePermissionSet)) {
                assertTrue(
                    scope in declared,
                    "runtime scope \"$scope\" (usePermissionSet=$usePermissionSet) is not declared in client metadata",
                )
            }
        }
    }

    @Test
    fun declaredMetadataSatisfiesAuthorizationServerValidation() {
        val declared = InkwellOAuthScopes.clientMetadataScopes()

        // The reference provider requires `atproto` and rejects duplicate tokens.
        assertTrue(OAuthScopes.ATPROTO in declared)
        assertEquals(declared.size, declared.toSet().size, "duplicate scope token in client metadata")
        assertTrue(declared.none { it.isBlank() })
    }

    @Test
    fun runtimeRequestHasNoDuplicateOrBlankTokens() {
        for (usePermissionSet in listOf(false, true)) {
            val scopes = InkwellOAuthScopes.runtimeScopes(usePermissionSet)
            assertEquals(scopes.size, scopes.toSet().size, "duplicate runtime scope token")
            assertTrue(scopes.none { it.isBlank() })
            assertTrue(scopes.none { " " in it }, "a scope token containing a space would split on the wire")
        }
    }

    @Test
    fun switchingToThePermissionSetGrantsExactlyTheSamePermissions() {
        // This is the reason no existing user has to re-authorize when the switch is made.
        assertEquals(
            StandardSitePermissionSets.effectivePermissions(
                InkwellOAuthScopes.runtimeScopes(usePermissionSet = false),
            ),
            StandardSitePermissionSets.effectivePermissions(
                InkwellOAuthScopes.runtimeScopes(usePermissionSet = true),
            ),
        )
    }

    @Test
    fun scopesThatCannotLiveInAPermissionSetAreAlwaysRequestedDirectly() {
        val neverInASet = listOf(OAuthScopes.ATPROTO, OAuthScopes.BLOB_ALL)
        assertEquals(neverInASet, InkwellOAuthScopes.ALWAYS_EXPLICIT)

        for (usePermissionSet in listOf(false, true)) {
            val scopes = InkwellOAuthScopes.runtimeScopes(usePermissionSet)
            assertTrue(neverInASet.all { it in scopes })
        }

        // No modelled permission set may smuggle a blob permission in.
        val expansions = StandardSitePermissionSets.AUTH_FULL_EXPANSION +
            StandardSitePermissionSets.AUTH_SOCIAL_EXPANSION
        assertTrue(expansions.none { it.startsWith("blob:") })
    }

    @Test
    fun scopesOutsideTheStandardSiteNamespaceAreNeverDelegatedToAPermissionSet() {
        // A permission set may only grant resources under its own NSID namespace.
        val outside = InkwellOAuthScopes.APP_RECORD_SCOPES + InkwellOAuthScopes.MODERATION_SCOPES
        assertTrue(outside.none { it.startsWith("repo:site.standard.") })

        for (usePermissionSet in listOf(false, true)) {
            val scopes = InkwellOAuthScopes.runtimeScopes(usePermissionSet)
            assertTrue(outside.all { it in scopes }, "an app-specific scope stopped being requested")
        }
    }

    @Test
    fun authFullExpansionMatchesThePublishedLexicon() {
        // site.standard.authFull carries one `repo` permission over these four
        // collections with no `action` array, which grants create + update + delete —
        // the same as the bare `repo:<collection>` scope strings.
        assertEquals(
            listOf(
                "repo:site.standard.publication",
                "repo:site.standard.document",
                "repo:site.standard.graph.subscription",
                "repo:site.standard.graph.recommend",
            ),
            StandardSitePermissionSets.AUTH_FULL_EXPANSION,
        )
        assertEquals("include:site.standard.authFull", StandardSitePermissionSets.INCLUDE_AUTH_FULL)
    }

    @Test
    fun authSocialIsALeastPrivilegeSubsetOfAuthFull() {
        assertEquals(
            listOf(
                "repo:site.standard.graph.subscription",
                "repo:site.standard.graph.recommend",
            ),
            StandardSitePermissionSets.AUTH_SOCIAL_EXPANSION,
        )
        assertEquals("include:site.standard.authSocial", StandardSitePermissionSets.INCLUDE_AUTH_SOCIAL)
        assertTrue(
            StandardSitePermissionSets.AUTH_SOCIAL_EXPANSION.all {
                it in StandardSitePermissionSets.AUTH_FULL_EXPANSION
            },
        )
    }

    @Test
    fun expansionIsOnlyClaimedForPermissionSetsInkwellActuallyModels() {
        assertEquals(
            StandardSitePermissionSets.AUTH_FULL_EXPANSION,
            StandardSitePermissionSets.expand(StandardSitePermissionSets.INCLUDE_AUTH_FULL),
        )
        assertEquals(
            StandardSitePermissionSets.AUTH_SOCIAL_EXPANSION,
            StandardSitePermissionSets.expand(StandardSitePermissionSets.INCLUDE_AUTH_SOCIAL),
        )
        assertNull(StandardSitePermissionSets.expand("include:com.example.someoneElsesSet"))
        assertNull(StandardSitePermissionSets.expand(OAuthScopes.REPO_DOCUMENT))
        assertNull(StandardSitePermissionSets.expand(OAuthScopes.AUTH_FULL), "bare NSID is not an include: scope")
    }

    @Test
    fun effectivePermissionsLeavesUnknownScopesUntouched() {
        val unknown = "include:com.example.someoneElsesSet"
        assertEquals(
            setOf(unknown, OAuthScopes.BLOB_ALL),
            StandardSitePermissionSets.effectivePermissions(listOf(unknown, OAuthScopes.BLOB_ALL)),
        )
    }
}
