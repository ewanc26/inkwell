package uk.ewancroft.inkwell.shared.oauth

/**
 * The single source of truth for Inkwell's OAuth scope request and for the maximum
 * scope its hosted `client-metadata.json` declares.
 *
 * ## The superset invariant
 *
 * The reference Authorization Server checks a requested scope by plain string
 * membership against the `scope` field of the client metadata document
 * (`oauth-provider/src/client/client.ts`: `Scope "<x>" is not declared in the client
 * metadata`). It does **not** expand permission sets before that check, and it does not
 * parse individual metadata scope tokens at registration time beyond requiring
 * `atproto` and rejecting duplicates
 * (`oauth-provider/src/client/client-manager.ts`).
 *
 * Two consequences drive the design here:
 *
 * 1. [runtimeScopes] must be a literal string subset of [clientMetadataScopes]. Not an
 *    "effectively equivalent" subset — the same strings.
 * 2. Declaring `include:site.standard.authFull` in client metadata is inert on an
 *    Authorization Server that does not understand permission sets, because nothing
 *    validates metadata scope tokens. So metadata can safely declare both the
 *    permission set and the granular scopes it expands to, and the runtime can pick
 *    either without a metadata redeploy.
 *
 * The three hosted copies of the metadata document must all carry
 * [clientMetadataScopeString]:
 *  - `website/src/routes/client-metadata.json/+server.ts` (the live one PDSes fetch)
 *  - `iOS/oauth/client-metadata.json`
 *  - `Android/docs/oauth/client-metadata.json`
 */
object InkwellOAuthScopes {

    /**
     * Scopes that cannot live in a permission set and must always be requested
     * directly.
     *
     * The permissions spec bars `blob` permissions from permission sets outright:
     * "Permissions of this type can not be included in permission sets, and must be
     * requested directly by client apps."
     */
    val ALWAYS_EXPLICIT: List<String> = listOf(
        OAuthScopes.ATPROTO,
        OAuthScopes.BLOB_ALL,
    )

    /**
     * Inkwell-specific and third-party record scopes.
     *
     * None of these sit under the `site.standard` namespace, so no Standard.site
     * permission set is allowed to grant them — permission sets may only address
     * resources in their own NSID hierarchy.
     */
    val APP_RECORD_SCOPES: List<String> = listOf(
        OAuthScopes.REPO_LEAFLET_COMMENT,
        OAuthScopes.REPO_USERINPUT_DISCUSSION,
        OAuthScopes.REPO_USER,
    )

    /**
     * Personal moderation: block records plus the Bluesky AppView RPCs behind the
     * mute/block/report UI on both platforms.
     */
    val MODERATION_SCOPES: List<String> = listOf(
        "repo:app.bsky.graph.block?action=create&action=delete",
        "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:com.atproto.moderation.createReport?aud=did:web:api.bsky.app%23bsky_appview",
    )

    /**
     * What the app actually asks for at authorization time.
     *
     * @param usePermissionSet when `true`, the four `repo:site.standard.*` scopes are
     *   replaced by the single `include:site.standard.authFull` invocation. The
     *   effective grant is identical either way — see
     *   [StandardSitePermissionSets.AUTH_FULL_EXPANSION].
     */
    fun runtimeScopes(
        usePermissionSet: Boolean = StandardSitePermissionSets.USE_PERMISSION_SET,
    ): List<String> =
        ALWAYS_EXPLICIT +
            StandardSitePermissionSets.standardSiteScopes(usePermissionSet) +
            APP_RECORD_SCOPES +
            MODERATION_SCOPES

    /**
     * The maximum scope declared in hosted client metadata: every scope the runtime
     * could request under either setting of
     * [StandardSitePermissionSets.USE_PERMISSION_SET].
     *
     * Declaring more than the app currently requests costs the user nothing — the
     * consent screen shows the *requested* scopes, not the declared ones — and removes
     * the deploy ordering hazard where a client update ships before the metadata that
     * authorizes it.
     */
    fun clientMetadataScopes(): List<String> =
        ALWAYS_EXPLICIT +
            StandardSitePermissionSets.AUTH_FULL_EXPANSION +
            StandardSitePermissionSets.INCLUDE_AUTH_FULL +
            APP_RECORD_SCOPES +
            MODERATION_SCOPES

    /** Space-joined form of [runtimeScopes], as sent in the OAuth `scope` parameter. */
    fun runtimeScopeString(
        usePermissionSet: Boolean = StandardSitePermissionSets.USE_PERMISSION_SET,
    ): String = runtimeScopes(usePermissionSet).joinToString(" ")

    /** Space-joined form of [clientMetadataScopes], as published in `client-metadata.json`. */
    val clientMetadataScopeString: String
        get() = clientMetadataScopes().joinToString(" ")
}
