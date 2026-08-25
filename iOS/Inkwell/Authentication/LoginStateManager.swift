//
//  LoginStateManager.swift
//  Inkwell
//
//  Central authentication and AT Protocol session manager.
//
//  Auth: OAuth 2.1 via OAuthenticator with Bluesky's AT Protocol OAuth flow
//        (PAR + PKCE + DPoP). Replaces the previous app-password +
//        Slingshot approach.
//  Identity: ATResolve for standard DNS/.well-known + PLC directory
//            resolution, replacing the Slingshot third-party service.
//  XRPC:    Direct HTTP calls authenticated via OAuthenticator for the
//           user's own PDS; unauthenticated URLSession for public repos.
//           ATProtoKit is kept for its type system (ATRecordProtocol,
//           UnknownType, ATURI, etc.).
//
//  This file holds the core class definition and shared state. Behaviour
//  is split across `LoginStateManager+*.swift` files in this directory.
//

import Foundation
import Observation
import OSLog
import ATProtoKit
import OAuthenticator
import ATResolve
import CryptoKit

// MARK: - Login State Manager

@MainActor
@Observable
final class LoginStateManager {
    let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Auth")
    // MARK: - Public State
    var isAuthenticated = false
    var currentHandle: String?
    var currentDID: String?
    var displayName: String?
    var avatarURL: URL?
    var errorMessage: String?

    /// `true` while the app is attempting to silently resume a previously
    /// authenticated session on launch.
    var isRestoringSession = true

    // MARK: - Storage Keys
    @ObservationIgnored let defaults: UserDefaults
    let storedHandleKey = "storedAccountHandle"
    let storedPDSKey = "storedAccountPDS"

    // MARK: - OAuth State
    @ObservationIgnored var authenticator: Authenticator?
    @ObservationIgnored var dpopKey: P256.Signing.PrivateKey?
    @ObservationIgnored var resolvedPDSURL: URL?

    // MARK: - Keychain
    @ObservationIgnored let loginStore = KeychainStore<Login>(
        service: "uk.ewancroft.Inkwell.oauth", account: "login"
    )
    @ObservationIgnored let dpopKeyStore = KeychainStore<Data>(
        service: "uk.ewancroft.Inkwell.oauth", account: "dpopKey"
    )

    // MARK: - Identity Resolver
    @ObservationIgnored let resolver = ATResolver(provider: URLSession.shared)

    // MARK: - Cross-repo Cache
    @ObservationIgnored var repositoryPDSURLs: [String: URL] = [:]
    @ObservationIgnored var _cachedSubscriptions: [SubscriptionEntry]?

    // MARK: - Client Metadata

    /// The OAuth client metadata for Inkwell.
    ///
    /// The `clientId` URL must serve the `client-metadata.json` file
    /// found in the repo's `oauth/` directory. In production this is
    /// `https://inkwell.ewancroft.uk/client-metadata.json`.
    ///
    /// Scopes follow the AT Protocol granular permission model
    /// (`atproto.com/specs/permission`). Inkwell requests access to:
    /// - Four `site.standard.*` collections (publications, documents,
    ///   subscriptions, recommends) for full CRUD.
    /// - `blob:*/*` for downloading media blobs via `sync.getBlob`.
    /// - `app.userinput.discussion` for in-app feedback.
    /// - Bluesky personal moderation RPCs plus create/delete access to
    ///   `app.bsky.graph.block`, matching the moderation UI on both platforms.
    var appCredentials: AppCredentials {
        AppCredentials(
            clientId: "https://inkwell.ewancroft.uk/client-metadata.json",
            clientPassword: "",
            scopes: [
                sharedOAuthScopeAtproto(),
                sharedOAuthScopeBlobAll(),
                sharedOAuthScopeRepoPublication(),
                sharedOAuthScopeRepoDocument(),
                sharedOAuthScopeRepoSubscription(),
                sharedOAuthScopeRepoRecommend(),
                sharedOAuthScopeRepoUserInputDiscussion(),
                "repo:app.bsky.graph.block?action=create&action=delete",
                "rpc:app.bsky.graph.muteActor?aud=did:web:api.bsky.app%23bsky_appview",
                "rpc:app.bsky.graph.unmuteActor?aud=did:web:api.bsky.app%23bsky_appview",
                "rpc:app.bsky.graph.getMutes?aud=did:web:api.bsky.app%23bsky_appview",
                "rpc:app.bsky.graph.getBlocks?aud=did:web:api.bsky.app%23bsky_appview"
            ],
            callbackURL: URL(string: "uk.ewancroft.inkwell:/callback")!
        )
    }

    // MARK: - Init

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }
}
