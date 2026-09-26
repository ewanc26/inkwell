package uk.ewancroft.inkwell.shared.oauth

/**
 * Standard.site's published AT Protocol permission sets, and the granular scopes
 * they expand to.
 *
 * A permission set is a `com.atproto.lexicon.schema` record of type `permission-set`
 * that an Authorization Server resolves at authorization time and expands into
 * granular permissions. Clients request one with an `include:<nsid>` scope.
 *
 * Verified against the live network on 2026-09-26 by resolving the lexicon the same
 * way a PDS does:
 *
 * ```
 * _lexicon.standard.site TXT -> did=did:plc:re3ebnp5v7ffagz6rb6xfei4
 *   -> plc.directory -> https://auriporia.us-west.host.bsky.network
 *   -> com.atproto.repo.getRecord(com.atproto.lexicon.schema, site.standard.authFull)
 * ```
 *
 * Both `site.standard.authFull` and `site.standard.authSocial` exist and carry a
 * single `repo` permission with no `action` array. Per the permissions spec, a `repo`
 * permission with no `action` grants create + update + delete — which is exactly what
 * the bare `repo:<collection>` scope strings Inkwell requests today already grant.
 *
 * **That equivalence is the reason adopting the permission set does not require
 * existing users to re-authorize:** the effective grant is byte-for-byte the same set
 * of capabilities, only the request string (and therefore the consent screen wording)
 * differs.
 *
 * ## Compatibility
 *
 * `include:` scope support landed in `@atproto/oauth-provider` 0.11.0 (2025-08-29);
 * granular `repo:`/`blob:`/`rpc:` scope support landed in 0.10.x (~2025-08-12). Inkwell
 * already requires the latter, so the only Authorization Servers that would accept
 * today's request but reject a permission-set request are ones pinned to a ~2.5 week
 * window of August 2025 builds.
 *
 * The residual risk is not the AS version but *resolution availability*: the spec says
 * "at the start of a session, if a permission set can not be resolved (and is not
 * already in a local cache), the auth request will fail". Requesting `include:` makes
 * login depend on the user's PDS being able to reach standard.site's DNS, plc.directory
 * and the publishing PDS. Raw scopes have no such dependency.
 *
 * Neither OAuth library Inkwell embeds (OAuthenticator on iOS, `AtOAuth` on Android)
 * exposes a seam to retry a failed pushed-authorization request with a different scope
 * string — Android fixes the scope string when the singleton `AtOAuth` is constructed in
 * `OAuthModule`, iOS fixes it in the `AppCredentials` it hands OAuthenticator — and the
 * authorization server metadata document has no field that advertises permission-set
 * support (`scopes_supported` is a static list that cannot enumerate the granular scope
 * space — bsky.social still advertises only `atproto` and the `transition:*` scopes while
 * happily accepting granular ones). So there is no honest runtime capability probe
 * available short of issuing a throwaway PAR before every login.
 *
 * Requesting the granular scopes *and* `include:site.standard.authFull` together is not a
 * graceful degradation either: an Authorization Server that cannot resolve or parse the
 * `include:` token rejects the whole authorization request rather than ignoring the token,
 * so asking for both makes login strictly more fragile than asking for neither.
 *
 * Because of that, [USE_PERMISSION_SET] defaults to `false`: the runtime keeps
 * requesting the granular scopes, while hosted client metadata declares *both* forms so
 * the switch is a one-line change that needs no metadata redeploy. See
 * [InkwellOAuthScopes.clientMetadataScopes].
 *
 * Declaring the unused `include:` token in client metadata is itself inert on every
 * Authorization Server version Inkwell can already authorize against: the reference
 * provider validates the metadata `scope` field only by splitting on spaces, requiring
 * `atproto`, and rejecting duplicate tokens — it never parses individual declared tokens.
 * Checked against `packages/oauth/oauth-provider/src/client/client-manager.ts` at tags
 * `@atproto/oauth-provider@0.10.0`, `@0.11.0`, and `main`.
 *
 * @see <a href="https://atproto.com/specs/permission">AT Protocol permissions spec</a>
 * @see <a href="https://standard.site/docs/permissions/">standard.site/docs/permissions</a>
 */
object StandardSitePermissionSets {

    /** Meta-resource prefix used to invoke a permission set as an OAuth scope. */
    const val INCLUDE_PREFIX = "include:"

    /** `include:site.standard.authFull` — publication, document, subscription, recommend. */
    const val INCLUDE_AUTH_FULL = INCLUDE_PREFIX + OAuthScopes.AUTH_FULL

    /** `include:site.standard.authSocial` — subscription and recommend only. */
    const val INCLUDE_AUTH_SOCIAL = INCLUDE_PREFIX + OAuthScopes.AUTH_SOCIAL

    /**
     * Granular scopes that `site.standard.authFull` expands to, in the order the
     * published lexicon lists its collections.
     */
    val AUTH_FULL_EXPANSION: List<String> = listOf(
        OAuthScopes.REPO_PUBLICATION,
        OAuthScopes.REPO_DOCUMENT,
        OAuthScopes.REPO_SUBSCRIPTION,
        OAuthScopes.REPO_RECOMMEND,
    )

    /** Granular scopes that `site.standard.authSocial` expands to. */
    val AUTH_SOCIAL_EXPANSION: List<String> = listOf(
        OAuthScopes.REPO_SUBSCRIPTION,
        OAuthScopes.REPO_RECOMMEND,
    )

    /**
     * Master switch for whether the runtime OAuth request uses the permission set
     * instead of the granular scopes it expands to.
     *
     * Kept `false` until a human has confirmed a real authorization flow against at
     * least one Bluesky-hosted PDS and one self-hosted PDS. Flipping it changes no
     * effective permission (see [AUTH_FULL_EXPANSION]); hosted client metadata already
     * declares both forms, so no metadata redeploy is needed.
     *
     * iOS mirrors this value as a Swift literal in `LoginStateManager.swift` because the
     * checked-in `InkwellShared.xcframework` predates this file; keep the two in sync
     * (same constraint as `sharedOAuthScopeRepoLeafletComment()` in `SharedKMP.swift`).
     */
    const val USE_PERMISSION_SET: Boolean = false

    /**
     * Expands an `include:` scope string into the granular scopes it grants, or returns
     * `null` if [scope] is not a permission set Inkwell knows about.
     *
     * Only the two Standard.site sets are modelled. Inkwell never requests a third
     * party's permission set, and guessing at an unknown set's contents would be worse
     * than admitting ignorance.
     */
    fun expand(scope: String): List<String>? = when (scope) {
        INCLUDE_AUTH_FULL -> AUTH_FULL_EXPANSION
        INCLUDE_AUTH_SOCIAL -> AUTH_SOCIAL_EXPANSION
        else -> null
    }

    /**
     * The effective granular permissions a list of requested scopes resolves to, with
     * every known permission set expanded in place and duplicates collapsed.
     *
     * Used to prove that flipping [USE_PERMISSION_SET] does not change what the user
     * actually grants, and that the runtime request stays within declared metadata.
     */
    fun effectivePermissions(scopes: List<String>): Set<String> =
        scopes.flatMap { expand(it) ?: listOf(it) }.toSet()

    /**
     * The Standard.site portion of Inkwell's OAuth request.
     *
     * Inkwell has exactly one sign-in path and it always leads to the Writer tab, so
     * `authSocial` is never the right request for this app: a reader who only subscribes
     * and recommends still shares the session with an editor that creates publication
     * and document records. Requesting `authSocial` would produce an honest-looking
     * consent screen for an app that then fails on first publish. `authSocial` becomes
     * applicable only if a genuinely read-only sign-in mode is added.
     */
    fun standardSiteScopes(usePermissionSet: Boolean = USE_PERMISSION_SET): List<String> =
        if (usePermissionSet) listOf(INCLUDE_AUTH_FULL) else AUTH_FULL_EXPANSION
}
