//
//  InkwellOAuthScopes.swift
//  Inkwell
//
//  Construction of the OAuth scope list Inkwell requests at authorization time.
//

import Foundation

/// Inkwell's OAuth scope request, and the rules that keep it aligned with the hosted
/// `client-metadata.json`.
///
/// This mirrors `InkwellOAuthScopes` / `StandardSitePermissionSets` in shared KMP
/// (`shared/src/commonMain/kotlin/uk/ewancroft/inkwell/shared/oauth/`). Kotlin is the
/// canonical definition; this file exists because the checked-in
/// `InkwellShared.xcframework` predates those types, exactly like the hardcoded
/// `sharedOAuthScopeRepoLeafletComment()` in `SharedKMP.swift`. Rebuild the framework
/// and this file collapses into a thin wrapper.
///
/// ## The superset invariant
///
/// An Authorization Server checks each requested scope by plain string membership
/// against the `scope` field of the client metadata document — it does not expand
/// permission sets before that check. So ``requested`` must be a literal string subset
/// of what `client-metadata.json` declares, and adding a scope here without publishing
/// it there breaks every login.
enum InkwellOAuthScopes {

    /// Whether the Standard.site block is requested as `include:site.standard.authFull`
    /// rather than as the four granular `repo:site.standard.*` scopes it expands to.
    ///
    /// **Mirror of `StandardSitePermissionSets.USE_PERMISSION_SET` in shared KMP — keep
    /// the two in sync.**
    ///
    /// Kept `false` until a human has confirmed a real authorization flow against both a
    /// Bluesky-hosted PDS and a self-hosted one. The published `site.standard.authFull`
    /// lexicon grants a `repo` permission over the same four collections with no
    /// `action` restriction, which is exactly what the granular scopes already grant, so
    /// flipping this changes the consent screen wording and nothing about the effective
    /// permissions — no existing user needs to re-authorize. Hosted client metadata
    /// already declares both forms, so no metadata redeploy is needed either.
    ///
    /// The reason it is not already `true`: requesting a permission set makes login
    /// depend on the user's PDS being able to *resolve* the `site.standard.authFull`
    /// lexicon (DNS → PLC → publishing PDS) at session start, and neither OAuthenticator
    /// nor the Android OAuth library exposes a seam to retry a rejected pushed
    /// authorization request with a different scope string. There is no field in
    /// `oauth-authorization-server` metadata that advertises permission-set support, so
    /// there is no honest pre-flight capability check either.
    static let usePermissionSet = false

    /// `include:site.standard.authFull` — the permission set covering publication,
    /// document, subscription and recommend records.
    static var includeAuthFull: String { "include:" + sharedOAuthScopeAuthFull() }

    /// `include:site.standard.authSocial` — subscriptions and recommends only.
    ///
    /// Unused: Inkwell has one sign-in path and it always leads to the Writer tab, so a
    /// session that cannot create publication or document records would fail on first
    /// publish. This becomes the right request only if a genuinely read-only sign-in
    /// mode is added.
    static var includeAuthSocial: String { "include:" + sharedOAuthScopeAuthSocial() }

    /// The granular scopes `site.standard.authFull` expands to, in the order the
    /// published lexicon lists its collections.
    static var standardSiteGranular: [String] {
        [
            sharedOAuthScopeRepoPublication(),
            sharedOAuthScopeRepoDocument(),
            sharedOAuthScopeRepoSubscription(),
            sharedOAuthScopeRepoRecommend(),
        ]
    }

    /// Scopes that can never live in a permission set.
    ///
    /// The permissions spec bars `blob` permissions from permission sets outright.
    static var alwaysExplicit: [String] {
        [sharedOAuthScopeAtproto(), sharedOAuthScopeBlobAll()]
    }

    /// Inkwell-specific and third-party record scopes.
    ///
    /// None sit under the `site.standard` namespace, so no Standard.site permission set
    /// is permitted to grant them — a set may only address resources in its own NSID
    /// hierarchy.
    static var appRecordScopes: [String] {
        [
            sharedOAuthScopeRepoLeafletComment(),
            sharedOAuthScopeRepoUserInputDiscussion(),
            "repo:uk.ewancroft.inkwell.user",
        ]
    }

    /// Personal moderation: block records plus the Bluesky AppView RPCs behind the
    /// mute/block/report UI.
    static let moderationScopes = [
        "repo:app.bsky.graph.block?action=create&action=delete",
        "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview",
        "rpc:com.atproto.moderation.createReport?aud=did:web:api.bsky.app%23bsky_appview",
    ]

    /// What the app actually asks for at authorization time.
    static func requested(usePermissionSet: Bool = InkwellOAuthScopes.usePermissionSet) -> [String] {
        alwaysExplicit
            + (usePermissionSet ? [includeAuthFull] : standardSiteGranular)
            + appRecordScopes
            + moderationScopes
    }

    /// The maximum scope declared in the hosted `client-metadata.json`: every scope the
    /// runtime could request under either setting of ``usePermissionSet``.
    static var clientMetadataDeclared: [String] {
        alwaysExplicit
            + standardSiteGranular
            + [includeAuthFull]
            + appRecordScopes
            + moderationScopes
    }

    /// Expands a known `include:` scope into the granular scopes it grants, or `nil` if
    /// it is not a permission set Inkwell models.
    ///
    /// Only the two Standard.site sets are modelled; guessing at a third party's set
    /// would be worse than admitting ignorance.
    static func expand(_ scope: String) -> [String]? {
        switch scope {
        case includeAuthFull: return standardSiteGranular
        case includeAuthSocial:
            return [sharedOAuthScopeRepoSubscription(), sharedOAuthScopeRepoRecommend()]
        default: return nil
        }
    }

    /// The effective granular permissions a scope list resolves to, with every known
    /// permission set expanded in place.
    ///
    /// Used to show that flipping ``usePermissionSet`` grants the same thing.
    static func effectivePermissions(_ scopes: [String]) -> Set<String> {
        Set(scopes.flatMap { expand($0) ?? [$0] })
    }
}
