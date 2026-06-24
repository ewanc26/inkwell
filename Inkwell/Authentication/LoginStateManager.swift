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

import Foundation
import Observation
import OSLog
import ATProtoKit
import OAuthenticator
import ATResolve
import CryptoKit

// MARK: - Supporting Types

/// A minimal Bluesky profile snapshot used for display-name / avatar.
struct ProfileSnapshot: Sendable {
    let displayName: String?
    let avatarURL: URL?
}

// MARK: - List Records Helpers

/// A single record entry in a `com.atproto.repo.listRecords` response page.
struct RepositoryRecord: Decodable, Sendable {
    let uri: String
    let cid: String?
    let value: UnknownType?
}

/// A `listRecords` response page that tolerates malformed records
/// (decodes what it can, drops the rest).
struct TolerantRecordPage: Decodable, Sendable {
    let cursor: String?
    let records: [RepositoryRecord]

    private enum CodingKeys: String, CodingKey {
        case cursor, records
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        cursor = try container.decodeIfPresent(String.self, forKey: .cursor)
        records = try container.decode(LossyRecordArray.self, forKey: .records).elements
    }
}

/// Decodes an array of `RepositoryRecord`, silently skipping any element
/// that fails to decode (e.g. because its `$type` is unrecognised).
private struct LossyRecordArray: Decodable, Sendable {
    let elements: [RepositoryRecord]

    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()
        var decoded: [RepositoryRecord] = []
        while !container.isAtEnd {
            let elementDecoder = try container.superDecoder()
            if let record = try? RepositoryRecord(from: elementDecoder) {
                decoded.append(record)
            }
        }
        elements = decoded
    }
}

// MARK: - Login State Manager

@MainActor
@Observable
final class LoginStateManager {
    private let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Auth")
    // MARK: - Public State
    private(set) var isAuthenticated = false
    private(set) var currentHandle: String?
    private(set) var currentDID: String?
    private(set) var displayName: String?
    private(set) var avatarURL: URL?
    private(set) var errorMessage: String?

    /// `true` while the app is attempting to silently resume a previously
    /// authenticated session on launch.
    private(set) var isRestoringSession = true

    // MARK: - Storage Keys
    @ObservationIgnored private let defaults: UserDefaults
    private let storedHandleKey = "storedAccountHandle"
    private let storedPDSKey = "storedAccountPDS"

    // MARK: - OAuth State
    @ObservationIgnored private var authenticator: Authenticator?
    @ObservationIgnored private var dpopKey: P256.Signing.PrivateKey?
    @ObservationIgnored private var resolvedPDSURL: URL?

    // MARK: - Keychain
    @ObservationIgnored private let loginStore = KeychainStore<Login>(
        service: "uk.ewancroft.Inkwell.oauth", account: "login"
    )
    @ObservationIgnored private let dpopKeyStore = KeychainStore<Data>(
        service: "uk.ewancroft.Inkwell.oauth", account: "dpopKey"
    )

    // MARK: - Identity Resolver
    @ObservationIgnored private let resolver = ATResolver(provider: URLSession.shared)

    // MARK: - Cross-repo Cache
    @ObservationIgnored private var repositoryPDSURLs: [String: URL] = [:]
    @ObservationIgnored private var _cachedSubscriptions: [SubscriptionEntry]?

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
    private var appCredentials: AppCredentials {
        AppCredentials(
            clientId: "https://inkwell.ewancroft.uk/client-metadata.json",
            clientPassword: "",
            scopes: [
                "atproto",
                "blob:*/*",
                "repo:site.standard.publication",
                "repo:site.standard.document",
                "repo:site.standard.graph.subscription",
                "repo:site.standard.graph.recommend"
            ],
            callbackURL: URL(string: "uk.ewancroft.inkwell:/callback")!
        )
    }

    // MARK: - Init

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    // MARK: - Authentication

    /// Starts the OAuth sign-in flow for the given handle.
    ///
    /// 1. Resolves the handle to a DID + PDS URL via ATResolve.
    /// 2. Fetches the PDS's OAuth server metadata.
    /// 3. Opens `ASWebAuthenticationSession` for user approval.
    /// 4. Exchanges the authorization code for DPoP-bound tokens.
    /// 5. Persists the `Login` and DPoP key in the Keychain.
    ///
    /// - Parameter handle: The AT Protocol handle (e.g. `alice.bsky.social`).
    /// - Returns: `true` if authentication succeeded.
    func signIn(handle: String) async -> Bool {
        let trimmed = handle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            errorMessage = "Enter your handle."
            return false
        }

        do {
            // 1. Resolve handle → DID + PDS
            guard let identity = try await resolver.resolveHandle(trimmed),
                  let pdsHost = identity.serviceEndpoint.flatMap({ URL(string: $0) })?.host,
                  let pdsURL = URL(string: identity.serviceEndpoint ?? "") else {
                errorMessage = "Could not resolve handle. Make sure it's a valid AT Protocol handle."
                return false
            }

            logger.info("[SignIn] resolved \(identity.handle) → DID \(identity.did), PDS \(pdsURL.absoluteString)")

            // 2. Load OAuth server metadata from the PDS
            let serverMetadata = try await ServerMetadata.load(for: pdsHost, provider: URLSession.defaultProvider)

            // 3. Generate or load DPoP key (must persist across sessions)
            let key: P256.Signing.PrivateKey
            if let existingData = try? dpopKeyStore.read(),
               let existingKey = try? P256.Signing.PrivateKey(rawRepresentation: existingData) {
                key = existingKey
            } else {
                key = P256.Signing.PrivateKey()
                try? dpopKeyStore.write(key.rawRepresentation)
            }

            // 4. Build token handling for Bluesky AT Protocol OAuth
            let tokenHandling = Bluesky.tokenHandling(
                account: trimmed,
                server: serverMetadata,
                jwtGenerator: DPoPJWTGenerator.generator(key: key),
                validator: { [weak self] tokenResponse, issuer in
                    // Verify that the token's subject (DID) resolves to a PDS
                    // whose issuer matches the token's issuer. This is a
                    // critical security check per AT Protocol OAuth spec.
                    guard let self else { return false }
                    guard let resolved = try? await self.resolver.resolveHandle(tokenResponse.sub),
                          let subPDSURL = resolved.serviceEndpoint.flatMap({ URL(string: $0) }),
                          subPDSURL.absoluteString.caseInsensitiveCompare(issuer) == .orderedSame
                            || issuer.contains(subPDSURL.host ?? "") else {
                        return false
                    }
                    return true
                }
            )

            // 5. Create Authenticator in manual mode to trigger auth.
            //    The debugLoader intercepts the first token request because
            //    eurosky.social burns auth codes on DPoP nonce mismatch.
            //    It holds the request, pre-flights the real nonce, returns
            //    a fake use_dpop_nonce to the library (which then retries
            //    with the correct nonce), and only sends the real request
            //    on the retry — so the code never reaches the server without
            //    the proper DPoP nonce.
            let tokenEndpoint = serverMetadata.tokenEndpoint
            let debugLoader: URLResponseProvider = { [logger] request in
                let (data, response) = try await URLSession.defaultProvider(request)
                if let http = response as? HTTPURLResponse, http.statusCode >= 400 {
                    let body = String(decoding: data, as: UTF8.self)
                    logger.error("[SignIn] token endpoint returned HTTP \(http.statusCode): \(body)")
                }
                return (data, response)
            }

            // Wrapping loader: intercept the first POST to the token
            // endpoint for nonce pre-warming.
            let loader: URLResponseProvider = { [logger] request in
                guard request.url?.absoluteString == tokenEndpoint,
                      request.httpMethod == "POST" else {
                    return try await debugLoader(request)
                }

                // First POST to token endpoint — don't send it yet.
                // Pre-flight to get the token endpoint's own DPoP nonce.
                var preflight = URLRequest(url: URL(string: tokenEndpoint)!)
                preflight.httpMethod = "GET"
                let (_, preResp) = try await URLSession.defaultProvider(preflight)
                guard let dpopNonce = (preResp as? HTTPURLResponse)?.value(forHTTPHeaderField: "DPoP-Nonce") else {
                    return try await debugLoader(request)
                }

                logger.info("[SignIn] token endpoint nonce pre-flighted, retrying with correct nonce")

                // Return a fake use_dpop_nonce response so the library's
                // DPoP layer caches the correct nonce and retries.
                let errorBody = Data("{\"error\":\"use_dpop_nonce\"}".utf8)
                let fakeResponse = HTTPURLResponse(
                    url: request.url!,
                    statusCode: 401,
                    httpVersion: nil,
                    headerFields: ["DPoP-Nonce": dpopNonce]
                )!
                return (errorBody, fakeResponse)
            }

            let config = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .manualOnly
            )
            let auth = Authenticator(config: config, urlLoader: loader)

            logger.info("[SignIn] starting ASWebAuthenticationSession…")
            try await auth.authenticate()
            logger.info("[SignIn] OAuth flow completed")

            // 6. Rebuild Authenticator in automatic mode for subsequent requests
            let autoConfig = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .automatic
            )
            self.authenticator = Authenticator(config: autoConfig)
            self.dpopKey = key
            self.resolvedPDSURL = pdsURL
            self.currentHandle = identity.handle
            self.currentDID = identity.did
            self.isAuthenticated = true
            self.errorMessage = nil
            self.defaults.set(identity.handle, forKey: storedHandleKey)
            self.defaults.set(pdsURL.absoluteString, forKey: storedPDSKey)

            // 7. Best-effort profile fetch (cosmetic — don't block sign-in)
            Task { [weak self] in
                guard let self else { return }
                do {
                    let profile = try await self.fetchProfile(did: identity.did)
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                } catch {
                    logger.error("[SignIn] profile fetch failed: \(error.localizedDescription)")
                }
            }

            return true
        } catch let error as DecodingError {
            // Decoding errors are ambiguous — surface the full context.
            let detail: String
            switch error {
            case .keyNotFound(let key, let context):
                detail = "missing key '\(key.stringValue)' (path: \(context.codingPath.map(\.stringValue)))"
            case .valueNotFound(let type, let context):
                detail = "null value for \(type) (path: \(context.codingPath.map(\.stringValue)))"
            case .typeMismatch(let type, let context):
                detail = "type mismatch, expected \(type) (path: \(context.codingPath.map(\.stringValue)))"
            case .dataCorrupted(let context):
                detail = "corrupted data: \(context.debugDescription)"
            @unknown default:
                detail = "unknown decoding error"
            }
            logger.error("[SignIn] DecodingError: \(detail)")
            logger.error("[SignIn] The token endpoint returned a response that doesn't match the expected format. This usually means the PDS rejected the token request. Check that https://inkwell.ewancroft.uk/client-metadata.json is publicly accessible and returns valid JSON with the correct client_id, redirect_uris, and grant_types.")
            errorMessage = "Your PDS rejected the token exchange. Make sure it supports AT Protocol OAuth and that https://inkwell.ewancroft.uk/client-metadata.json is publicly accessible."
            clearSession()
            return false
        } catch {
            logger.error("[SignIn] error: \(type(of: error)) — \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            clearSession()
            return false
        }
    }

    /// Attempts to silently resume a previously authenticated session
    /// using the OAuth tokens stored in the Keychain.
    ///
    /// Call this once on launch from the root view's `.task`.
    func restoreSessionIfPossible() async {
        defer { isRestoringSession = false }

        guard let storedHandle = defaults.string(forKey: storedHandleKey),
              let storedPDS = defaults.string(forKey: storedPDSKey),
              let pdsURL = URL(string: storedPDS) else {
            return
        }

        // Load DPoP key and stored Login
        guard let dpopKeyData = try? dpopKeyStore.read(),
              let key = try? P256.Signing.PrivateKey(rawRepresentation: dpopKeyData),
              let storedLogin = try? loginStore.read() else {
            logger.info("[RestoreSession] no stored session found")
            clearSession()
            return
        }

        // Quick expiry check — don't bother building the authenticator
        // if the access token is already expired and there's no refresh token.
        if !storedLogin.accessToken.valid && storedLogin.refreshToken == nil {
            logger.info("[RestoreSession] access token expired, no refresh token")
            clearSession()
            return
        }

        do {
            // Resolve identity for fresh DID
            guard let identity = try? await resolver.resolveHandle(storedHandle) else {
                logger.error("[RestoreSession] identity resolution failed")
                clearSession()
                return
            }

            guard let pdsHost = pdsURL.host else {
                clearSession()
                return
            }

            let serverMetadata = try await ServerMetadata.load(for: pdsHost, provider: URLSession.defaultProvider)

            let tokenHandling = Bluesky.tokenHandling(
                account: storedHandle,
                server: serverMetadata,
                jwtGenerator: DPoPJWTGenerator.generator(key: key),
                validator: { _, _ in true } // Already validated during initial sign-in
            )

            let config = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .automatic
            )
            let auth = Authenticator(config: config)

            self.authenticator = auth
            self.dpopKey = key
            self.resolvedPDSURL = pdsURL
            self.currentHandle = identity.handle
            self.currentDID = identity.did
            self.isAuthenticated = true
            self.errorMessage = nil

            logger.info("[RestoreSession] session restored for \(identity.handle)")

            // Verify the restored tokens are still accepted by the PDS
            // before declaring the session live. This catches server-side
            // revocation before the user tries to write anything — reads
            // use unauthenticated requests so they would succeed either
            // way, masking a broken token until the first mutation.
            do {
                _ = try await authenticatedData(path: "/xrpc/com.atproto.server.getSession")
                logger.info("[RestoreSession] token verification succeeded")
            } catch {
                logger.warning("[RestoreSession] token verification failed (\(error.localizedDescription)) — clearing session")
                clearSession()
                return
            }

            Task { [weak self] in
                guard let self else { return }
                do {
                    let profile = try await self.fetchProfile(did: identity.did)
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                } catch {
                    logger.error("[RestoreSession] profile fetch failed: \(error.localizedDescription)")
                }
            }
        } catch {
            logger.error("[RestoreSession] failed: \(error.localizedDescription)")
            clearSession()
        }
    }

    /// Signs the user out, clearing all stored tokens and state.
    func signOut() {
        clearSession(clearStoredAccount: true)
        errorMessage = nil
        Task {
            try? loginStore.delete()
            try? dpopKeyStore.delete()
        }
    }

    // MARK: - XRPC Helpers

    /// Makes an authenticated request to the user's PDS.
    ///
    /// - Parameters:
    ///   - path: The XRPC path (e.g. `/xrpc/com.atproto.repo.listRecords`).
    ///   - method: The HTTP method.
    ///   - body: Optional JSON-encoded request body.
    ///   - queryItems: Optional URL query parameters.
    /// - Returns: The response data.
    private func authenticatedData(
        path: String,
        method: String = "GET",
        body: Data? = nil,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> Data {
        guard let authenticator, let pdsURL = resolvedPDSURL else {
            throw LoginError.notAuthenticated
        }

        var components = URLComponents(
            url: pdsURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        )
        if let queryItems, !queryItems.isEmpty {
            components?.queryItems = queryItems
        }
        guard let url = components?.url else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await authenticator.response(for: request)

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            logger.error("[authenticatedData] HTTP \(status) for \(url.absoluteString)")
            throw LoginError.httpError(status: status)
        }

        return data
    }

    /// Makes an unauthenticated request to a remote PDS (for public records).
    private func unauthenticatedData(
        pdsURL: URL,
        path: String,
        method: String = "GET",
        body: Data? = nil,
        queryItems: [URLQueryItem]? = nil
    ) async throws -> Data {
        var components = URLComponents(
            url: pdsURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        )
        if let queryItems, !queryItems.isEmpty {
            components?.queryItems = queryItems
        }
        guard let url = components?.url else {
            throw URLError(.badURL)
        }

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 8
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await withRetry {
            try await URLSession.shared.data(for: request)
        }

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            logger.error("[unauthenticatedData] HTTP \(status) for \(url.absoluteString)")
            throw LoginError.httpError(status: status)
        }

        return data
    }

    // MARK: - Retry

    /// Retries an async operation with exponential backoff.
    ///
    /// Uses jittered exponential backoff (100ms → 200ms → 400ms → 800ms)
    /// for transient network errors. Non-retryable errors (e.g. 401, 403,
    /// invalid URIs) are rethrown immediately.
    private func withRetry<T>(
        maxAttempts: Int = 4,
        operation: () async throws -> T
    ) async throws -> T {
        var attempt = 0
        var lastError: Error?

        while attempt < maxAttempts {
            do {
                return try await operation()
            } catch let error as URLError where error.isTransient {
                attempt += 1
                lastError = error
                guard attempt < maxAttempts else { throw error }
                let delay = Double(1 << min(attempt, 4)) * 0.1  // 0.1, 0.2, 0.4, 0.8s
                try? await Task.sleep(for: .seconds(delay))
            } catch LoginError.httpError(let status) where (500...599).contains(status) {
                attempt += 1
                lastError = LoginError.httpError(status: status)
                guard attempt < maxAttempts else { throw lastError! }
                let delay = Double(1 << min(attempt, 4)) * 0.1
                try? await Task.sleep(for: .seconds(delay))
            }
        }

        throw lastError ?? LoginError.httpError(status: 0)
    }

    // MARK: - Profile

    /// Fetches a Bluesky profile for the given DID via the public API.
    ///
    /// Delegates to ``BSkyProfileFetcher`` so the full profile model is
    /// available to the app and cached globally.
    func fetchProfile(did: String) async throws -> ProfileSnapshot {
        let profile = try await BSkyProfileFetcher.fetchProfile(did: did)
        return ProfileSnapshot(
            displayName: profile.displayName,
            avatarURL: profile.avatar.flatMap { URL(string: $0) }
        )
    }

    // MARK: - Blob Download

    /// Downloads raw bytes of a blob by its CID from the user's PDS.
    func downloadBlob(cid: String) async throws -> Data {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }
        return try await downloadBlob(cid: cid, fromDID: did)
    }

    /// Downloads a blob from the PDS hosting the specified repository.
    func downloadBlob(cid: String, fromDID did: String) async throws -> Data {
        let pdsURL = try await repositoryPDSURL(for: did)

        if did == currentDID {
            return try await authenticatedData(
                path: "/xrpc/com.atproto.sync.getBlob",
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ]
            )
        } else {
            return try await unauthenticatedData(
                pdsURL: pdsURL,
                path: "/xrpc/com.atproto.sync.getBlob",
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ]
            )
        }
    }

    // MARK: - Record CRUD

    /// Creates an AT Protocol record in the user's repository.
    @discardableResult
    func createRecord(
        collection: String,
        record: UnknownType,
        shouldValidate: Bool = false
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }

        struct CreateRecordBody: Encodable {
            let repo: String
            let collection: String
            let record: UnknownType
            let validate: Bool

            enum CodingKeys: String, CodingKey {
                case repo, collection, record, validate
            }
        }

        let body = CreateRecordBody(
            repo: did,
            collection: collection,
            record: record,
            validate: shouldValidate
        )
        let bodyData = try JSONEncoder().encode(body)

        let data = try await authenticatedData(
            path: "/xrpc/com.atproto.repo.createRecord",
            method: "POST",
            body: bodyData
        )

        return try JSONDecoder().decode(ComAtprotoLexicon.Repository.StrongReference.self, from: data)
    }

    /// Deletes an AT Protocol record from the user's repository.
    func deleteRecord(collection: String, recordKey: String) async throws {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }

        struct DeleteRecordBody: Encodable {
            let repo: String
            let collection: String
            let rkey: String

            enum CodingKeys: String, CodingKey {
                case repo, collection, rkey
            }
        }

        let bodyData = try JSONEncoder().encode(
            DeleteRecordBody(repo: did, collection: collection, rkey: recordKey)
        )

        _ = try await authenticatedData(
            path: "/xrpc/com.atproto.repo.deleteRecord",
            method: "POST",
            body: bodyData
        )
    }

    /// Fetches and decodes a single record from a repository.
    func getRepositoryRecord(
        from did: String,
        collection: String,
        recordKey: String
    ) async throws -> (uri: String, cid: String?, value: UnknownType?) {
        let pdsURL = try await repositoryPDSURL(for: did)

        let queryItems = [
            URLQueryItem(name: "repo", value: did),
            URLQueryItem(name: "collection", value: collection),
            URLQueryItem(name: "rkey", value: recordKey),
        ]

        let data: Data
        if did == currentDID {
            data = try await authenticatedData(
                path: "/xrpc/com.atproto.repo.getRecord",
                queryItems: queryItems
            )
        } else {
            data = try await unauthenticatedData(
                pdsURL: pdsURL,
                path: "/xrpc/com.atproto.repo.getRecord",
                queryItems: queryItems
            )
        }

        struct GetRecordOutput: Decodable {
            let uri: String
            let cid: String?
            let value: UnknownType?
        }
        let output = try JSONDecoder().decode(GetRecordOutput.self, from: data)
        return (output.uri, output.cid, output.value)
    }

    // MARK: - List Records (cross-repo)

    /// Lists all records of a given collection from a repository,
    /// following pagination cursors up to `maximumCount`.
    func listAllRecords(
        from did: String,
        collection: String,
        maximumCount: Int = 1_000
    ) async throws -> [RepositoryRecord] {
        let pdsURL = try await repositoryPDSURL(for: did)

        var allRecords: [RepositoryRecord] = []
        var cursor: String?

        repeat {
            var queryItems = [
                URLQueryItem(name: "repo", value: did),
                URLQueryItem(name: "collection", value: collection),
                URLQueryItem(name: "limit", value: String(min(100, maximumCount - allRecords.count))),
            ]
            if let cursor {
                queryItems.append(URLQueryItem(name: "cursor", value: cursor))
            }

            // Try unauthenticated first — most PDS servers allow public
            // listRecords for standard.site collections. If the PDS
            // requires authentication (401/403), retry with auth.
            // This avoids DPoP nonce exhaustion when multiple
            // collections are listed in sequence for the same DID.
            let data: Data
            if did == currentDID {
                do {
                    data = try await unauthenticatedData(
                        pdsURL: pdsURL,
                        path: "/xrpc/com.atproto.repo.listRecords",
                        queryItems: queryItems
                    )
                } catch LoginError.httpError(let status) where status == 401 || status == 403 {
                    data = try await authenticatedData(
                        path: "/xrpc/com.atproto.repo.listRecords",
                        queryItems: queryItems
                    )
                }
            } else {
                data = try await unauthenticatedData(
                    pdsURL: pdsURL,
                    path: "/xrpc/com.atproto.repo.listRecords",
                    queryItems: queryItems
                )
            }

            let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
            logger.info("[listAllRecords] \(collection): raw JSON returned \(page.records.count) records (cursor: \(page.cursor ?? "nil"))")
            let withValues = page.records.filter { $0.value != nil }
            logger.info("[listAllRecords] \(collection): \(withValues.count)/\(page.records.count) records have non-nil value")
            allRecords.append(contentsOf: page.records)
            cursor = page.cursor
        } while cursor != nil && allRecords.count < maximumCount

        return Array(allRecords.prefix(maximumCount))
    }

    /// Fetches a single page of records from a repository.
    ///
    /// Unlike ``listAllRecords(from:collection:maximumCount:)``, this makes
    /// exactly one HTTP request — no pagination loop. The caller is responsible
    /// for advancing the cursor to fetch subsequent pages.
    ///
    /// - Parameters:
    ///   - did: The DID whose repository to query.
    ///   - collection: The NSID of the collection to list.
    ///   - limit: The number of records per page (1–100, default 25).
    ///   - cursor: An opaque cursor from a previous page, or `nil` for the first page.
    /// - Returns: A tuple of the decoded records and an optional cursor for the next page.
    func listRecordsPage(
        from did: String,
        collection: String,
        limit: Int = 25,
        cursor: String? = nil
    ) async throws -> (records: [RepositoryRecord], cursor: String?) {
        let pdsURL = try await repositoryPDSURL(for: did)

        var queryItems = [
            URLQueryItem(name: "repo", value: did),
            URLQueryItem(name: "collection", value: collection),
            URLQueryItem(name: "limit", value: String(min(limit, 100))),
        ]
        if let cursor {
            queryItems.append(URLQueryItem(name: "cursor", value: cursor))
        }

        let data: Data
        if did == currentDID {
            do {
                data = try await unauthenticatedData(
                    pdsURL: pdsURL,
                    path: "/xrpc/com.atproto.repo.listRecords",
                    queryItems: queryItems
                )
            } catch LoginError.httpError(let status) where status == 401 || status == 403 {
                data = try await authenticatedData(
                    path: "/xrpc/com.atproto.repo.listRecords",
                    queryItems: queryItems
                )
            }
        } else {
            data = try await unauthenticatedData(
                pdsURL: pdsURL,
                path: "/xrpc/com.atproto.repo.listRecords",
                queryItems: queryItems
            )
        }

        let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
        logger.info("[listRecordsPage] \(collection): \(page.records.count) records (cursor: \(page.cursor ?? "nil"))")
        return (page.records, page.cursor)
    }

    // MARK: - Publications

    /// Fetches all of the user's publication records.
    func fetchPublications() async throws -> [SiteStandardLexicon.PublicationRecord] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)
        let decoded = records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
        logger.info("[fetchPublications] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchPublications] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Fetches publications from any user's repository.
    func fetchPublications(fromDID did: String) async throws -> [PublicationEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
                .map { PublicationEntry(uri: record.uri, authorDID: did, record: $0) }
        }
        logger.info("[fetchPublicationsEntry] \(records.count) raw → \(decoded.count) decoded PublicationEntry")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchPublicationsEntry] 0/\(records.count) records decoded for \(did)")
        }
        return decoded
    }

    /// Fetches publications from the current user's repository with URIs.
    func fetchPublicationsWithURIs() async throws -> [PublicationEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        return try await fetchPublications(fromDID: did)
    }

    /// Fetches one publication by AT-URI.
    func fetchPublication(uri: String) async throws -> PublicationEntry {
        guard let parsed = ATURI.parse(uri),
              parsed.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw LoginError.invalidURI
        }
        let (recordURI, _, value) = try await getRepositoryRecord(
            from: parsed.did, collection: parsed.collection, recordKey: parsed.recordKey
        )
        guard let publication = value?.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) else {
            throw LoginError.unexpectedRecordType
        }
        return PublicationEntry(uri: recordURI, authorDID: parsed.did, record: publication)
    }

    /// Creates a publication record.
    @discardableResult
    func createPublication(
        url: String,
        name: String,
        description: String?
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        let record = UnknownType.record(
            SiteStandardLexicon.PublicationRecord(url: url, name: name, description: description)
        )
        return try await createRecord(
            collection: SiteStandardLexicon.PublicationRecord.type,
            record: record
        )
    }

    // MARK: - Documents

    /// Fetches all of the user's document records.
    func fetchDocuments() async throws -> [SiteStandardLexicon.DocumentRecord] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.DocumentRecord.type)
        let decoded = records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
        logger.info("[fetchDocuments] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchDocuments] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Fetches documents from any user's repository.
    func fetchDocuments(fromDID did: String) async throws -> [DocumentEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.DocumentRecord.type)
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
                .map { DocumentEntry(uri: record.uri, authorDID: did, record: $0) }
        }
        logger.info("[fetchDocumentsEntry] \(records.count) raw → \(decoded.count) decoded DocumentEntry")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchDocumentsEntry] 0/\(records.count) records decoded for \(did)")
        }
        return decoded
    }

    /// Fetches documents from the current user's repository with URIs.
    func fetchDocumentsWithURIs() async throws -> [DocumentEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        return try await fetchDocuments(fromDID: did)
    }

    /// Fetches one document by AT-URI.
    func fetchDocument(uri: String) async throws -> DocumentEntry {
        guard let parsed = ATURI.parse(uri),
              parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw LoginError.invalidURI
        }
        let (recordURI, _, value) = try await getRepositoryRecord(
            from: parsed.did, collection: parsed.collection, recordKey: parsed.recordKey
        )
        guard let document = value?.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
            throw LoginError.unexpectedRecordType
        }
        return DocumentEntry(uri: recordURI, authorDID: parsed.did, record: document)
    }

    /// Creates and publishes a new document record.
    @discardableResult
    func createDocument(
        title: String,
        description: String?,
        path: String?,
        site: String,
        markdown: String,
        provider: ContentProvider,
        previousContent: UnknownType? = nil
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard currentDID != nil else { throw LoginError.notAuthenticated }

        let ctx = WriteContext(previousContent: previousContent)
        guard let contentRecord = provider.fromMarkdown(markdown, ctx: ctx) else {
            throw LoginError.contentConversionFailed
        }

        guard (ATURI.parse(site)?.collection == SiteStandardLexicon.PublicationRecord.type) ||
                (URL(string: site)?.scheme?.lowercased() == "https") else {
            throw LoginError.invalidURI
        }

        let normalizedPath = path.flatMap { value -> String? in
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return nil }
            return trimmed.hasPrefix("/") ? trimmed : "/\(trimmed)"
        }
        let plainText = (try? AttributedString(markdown: markdown))
            .map { String($0.characters) }
            .flatMap { $0.isEmpty ? nil : $0 }

        var normalizedSite = site
        while normalizedSite.hasSuffix("/") {
            normalizedSite.removeLast()
        }

        let document = SiteStandardLexicon.DocumentRecord(
            site: normalizedSite,
            title: title,
            publishedAt: Date(),
            path: normalizedPath,
            description: description,
            coverImage: nil,
            content: contentRecord,
            textContent: plainText
        )

        return try await createRecord(
            collection: SiteStandardLexicon.DocumentRecord.type,
            record: UnknownType.record(document)
        )
    }

    // MARK: - Subscriptions

    /// Creates a `site.standard.graph.subscription` record.
    @discardableResult
    func createSubscription(publicationURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard ATURI.parse(publicationURI)?.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw LoginError.invalidURI
        }
        let subscription = SiteStandardLexicon.Graph.SubscriptionRecord(
            publication: publicationURI, createdAt: Date()
        )
        return try await createRecord(
            collection: SiteStandardLexicon.Graph.SubscriptionRecord.type,
            record: UnknownType.record(subscription)
        )
    }

    /// Fetches the user's subscriptions. Results are cached in-memory for the
    /// lifetime of the session so concurrent callers (e.g. BrowseDocumentsView
    /// and NotificationManager) don't race DPoP nonces against each other.
    func fetchSubscriptions() async throws -> [SubscriptionEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }

        // Return a cached snapshot when available — avoids redundant
        // authenticated listRecords calls that would collide on DPoP nonces.
        if let cached = _cachedSubscriptions { return cached }

        let records = try await listAllRecords(
            from: did, collection: SiteStandardLexicon.Graph.SubscriptionRecord.type
        )
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.Graph.SubscriptionRecord.self) }
                .map {
                    let rkey = ATURI.parse(record.uri)?.recordKey ?? ""
                    return SubscriptionEntry(uri: record.uri, recordKey: rkey, record: $0)
                }
        }
        _cachedSubscriptions = decoded
        logger.info("[fetchSubscriptions] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchSubscriptions] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Deletes a subscription record.
    func deleteSubscription(recordKey: String) async throws {
        try await deleteRecord(
            collection: SiteStandardLexicon.Graph.SubscriptionRecord.type,
            recordKey: recordKey
        )
    }

    // MARK: - Recommends

    /// Creates a `site.standard.graph.recommend` record.
    @discardableResult
    func createRecommend(documentURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard ATURI.parse(documentURI)?.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw LoginError.invalidURI
        }
        let recommend = SiteStandardLexicon.Graph.RecommendRecord(
            document: documentURI, createdAt: Date()
        )
        return try await createRecord(
            collection: SiteStandardLexicon.Graph.RecommendRecord.type,
            record: UnknownType.record(recommend)
        )
    }

    /// Fetches the **current user's** recommends (local repo only).
    ///
    /// Used to determine whether the signed-in user has already recommended
    /// a given document. For a global recommend count or list across all
    /// repos, use ``fetchAllRecommends(for:)`` instead.
    func fetchRecommends() async throws -> [RecommendEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(
            from: did, collection: SiteStandardLexicon.Graph.RecommendRecord.type
        )
        return records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.Graph.RecommendRecord.self) }
                .map {
                    let rkey = ATURI.parse(record.uri)?.recordKey ?? ""
                    return RecommendEntry(uri: record.uri, recordKey: rkey, record: $0)
                }
        }
    }

    /// Fetches **all** recommend records referencing a document, across the
    /// entire AT Protocol network, using the Constellation backlink index.
    ///
    /// Each discovered backlink is hydrated from the recommender's PDS.
    /// Results are deduplicated by URI.
    func fetchAllRecommends(for documentURI: String) async -> [RecommendEntry] {
        let backlinks = await ConstellationClient.getRecommendBacklinks(
            documentURI: documentURI
        )

        var seen = Set<String>()
        var recommends: [RecommendEntry] = []

        await withTaskGroup(of: RecommendEntry?.self) { group in
            for backlink in backlinks {
                let uri = backlink.recordURI
                guard seen.insert(uri).inserted else { continue }

                group.addTask { [backlink] in
                    guard let (recordURI, _, value) = try? await self.getRepositoryRecord(
                        from: backlink.did,
                        collection: backlink.collection,
                        recordKey: backlink.rkey
                    ),
                    let record = value?.getRecord(ofType: SiteStandardLexicon.Graph.RecommendRecord.self),
                    record.document == documentURI else {
                        return nil
                    }
                    return RecommendEntry(uri: recordURI, recordKey: backlink.rkey, record: record)
                }
            }
            for await result in group {
                if let entry = result {
                    recommends.append(entry)
                }
            }
        }

        return recommends
    }

    /// Returns the total count of recommends for a document (across all
    /// repos), using Constellation discovery without hydrating records.
    func fetchRecommendCount(for documentURI: String) async -> Int {
        // A single page is sufficient for counting; the first response
        // includes a `total` field. We ask for 1 record to minimise bytes.
        let result = try? await ConstellationClient.getBacklinks(
            subject: documentURI,
            source: "site.standard.graph.recommend:document",
            limit: 1
        )
        // The total count is available from the Constellation API but we
        // didn't model it. Fall back: paginate and count.
        guard let result else { return 0 }
        if result.cursor == nil {
            return result.backlinks.count
        }
        // Multi-page case — paginate fully.
        let all = await ConstellationClient.getRecommendBacklinks(
            documentURI: documentURI
        )
        return all.count
    }

    /// Returns the AT-URIs of Bluesky posts that link to the given document
    /// URL via facets or external embeds, using Constellation.
    ///
    /// This mirrors leaflet.pub's `getConstellationBacklinks()`.
    func fetchDocumentMentionURIs(for documentURL: String) async -> [String] {
        let backlinks = await ConstellationClient.getDocumentMentionBacklinks(
            url: documentURL
        )
        return backlinks.map(\.recordURI)
    }

    /// Deletes a recommend record.
    func deleteRecommend(recordKey: String) async throws {
        try await deleteRecord(
            collection: SiteStandardLexicon.Graph.RecommendRecord.type,
            recordKey: recordKey
        )
    }

    // MARK: - Comments

    /// Resolves blob-backed Leaflet pages when a document's content uses
    /// `blobPages` (large posts offloaded to a PDS blob). Returns a new
    /// UnknownType with the resolved pages inlined, or the original content
    /// when no blobPages are present.
    func resolveBlobPages(in content: UnknownType?) async -> UnknownType? {
        guard let content,
              let leaflet = content.getRecord(ofType: LeafletContent.self),
              let blobRef = leaflet.blobPages else {
            return content
        }
        do {
            let blobData = try await downloadBlob(cid: blobRef.reference.link)
            let pages = try JSONDecoder().decode([LeafletPage].self, from: blobData)
            let resolved = LeafletContent(pages: pages, blobPages: nil)
            return UnknownType.record(resolved)
        } catch {
            logger.error("[resolveBlobPages] failed to fetch blob pages: \(error)")
            return content  // fall back to whatever inline pages exist
        }
    }

    /// Fetches `pub.leaflet.comment` records referencing the given document
    /// as their `subject`, newest first.
    ///
    /// Uses two sources, merged:
    ///
    /// 1. **Constellation** (microcosm.blue) — a global AT Protocol backlink
    ///    index that discovers comment records across *all* repositories.
    ///    This catches comments from any user, not just the current one.
    ///    Each discovered backlink is hydrated from the commenter's PDS via
    ///    `com.atproto.repo.getRecord`.
    ///
    /// 2. **Local PDS** — the current user's own repo, as a fast path for
    ///    the user's own comments (avoids the Constellation round-trip and
    ///    PDS hydration for those records).
    ///
    /// Constellation results are deduplicated against local results by URI.
    func fetchComments(documentURI: String) async throws -> [CommentEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }

        // 1. Local: fetch the current user's own comments from their repo.
        let localRecords = (try? await listAllRecords(
            from: did, collection: PubLeafletComment.type
        )) ?? []

        var seen = Set<String>()
        var comments: [CommentEntry] = []

        for record in localRecords {
            guard let value = record.value,
                  let comment = value.getRecord(ofType: PubLeafletComment.self),
                  comment.subject == documentURI,
                  !seen.contains(record.uri) else { continue }
            seen.insert(record.uri)
            comments.append(CommentEntry(
                uri: record.uri,
                recordKey: ATURI.parse(record.uri)?.recordKey ?? "",
                record: comment
            ))
        }

        // 2. Constellation: discover comments from ALL repos.
        let backlinks = await ConstellationClient.getCommentBacklinks(
            documentURI: documentURI
        )

        // Hydrate each backlink from the commenter's PDS.
        await withTaskGroup(of: CommentEntry?.self) { group in
            for backlink in backlinks {
                let uri = backlink.recordURI
                guard !seen.contains(uri) else { continue }
                seen.insert(uri)

                group.addTask { [backlink, documentURI] in
                    guard let (recordURI, _, value) = try? await self.getRepositoryRecord(
                        from: backlink.did,
                        collection: backlink.collection,
                        recordKey: backlink.rkey
                    ),
                    let comment = value?.getRecord(ofType: PubLeafletComment.self),
                    comment.subject == documentURI else {
                        return nil
                    }
                    return CommentEntry(
                        uri: recordURI,
                        recordKey: backlink.rkey,
                        record: comment
                    )
                }
            }
            for await result in group {
                if let entry = result {
                    comments.append(entry)
                }
            }
        }

        return comments.sorted { $0.record.createdAt > $1.record.createdAt }
    }

    /// Creates a `pub.leaflet.comment` record.
    @discardableResult
    func createComment(
        subject: String,
        plaintext: String,
        replyTo: String? = nil,
        onPage: String? = nil
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        let comment = PubLeafletComment(
            subject: subject,
            plaintext: plaintext,
            reply: replyTo.map { PubLeafletComment.ReplyRef(parent: $0) },
            onPage: onPage
        )
        return try await createRecord(
            collection: PubLeafletComment.type,
            record: UnknownType.record(comment)
        )
    }

    /// Deletes a comment record by its record key.
    func deleteComment(recordKey: String) async throws {
        try await deleteRecord(
            collection: PubLeafletComment.type,
            recordKey: recordKey
        )
    }

    // MARK: - PDS Resolution

    /// Resolves the PDS URL for a given DID, caching the result.
    private func repositoryPDSURL(for did: String) async throws -> URL {
        if let cached = repositoryPDSURLs[did] { return cached }

        // Own DID — use the stored PDS.
        if did == currentDID,
           let storedPDS = defaults.string(forKey: storedPDSKey),
           let url = URL(string: storedPDS) {
            repositoryPDSURLs[did] = url
            return url
        }

        // Resolve via ATResolve.
        let identity = try await resolver.resolveHandle(did)
        guard let pdsString = identity?.serviceEndpoint,
              let url = URL(string: pdsString) else {
            throw LoginError.pdsResolutionFailed
        }
        repositoryPDSURLs[did] = url
        return url
    }

    // MARK: - Helpers

    private func makeLoginStorage() -> LoginStorage {
        // KeychainStore is a value type — capture a copy so the closures
        // can use it from any isolation domain without hopping to @MainActor.
        let store = loginStore
        return LoginStorage(
            retrieveLogin: { try await store.read() },
            storeLogin: { try await store.write($0) },
            clearLogin: { try await store.delete() }
        )
    }

    private func clearSession(clearStoredAccount: Bool = false) {
        isAuthenticated = false
        currentHandle = nil
        currentDID = nil
        displayName = nil
        avatarURL = nil
        authenticator = nil
        dpopKey = nil
        resolvedPDSURL = nil
        repositoryPDSURLs.removeAll()
        _cachedSubscriptions = nil

        if clearStoredAccount {
            defaults.removeObject(forKey: storedHandleKey)
            defaults.removeObject(forKey: storedPDSKey)
        }
    }
}

// MARK: - Error

enum LoginError: LocalizedError {
    case notAuthenticated
    case invalidURI
    case unexpectedRecordType
    case contentConversionFailed
    case pdsResolutionFailed
    case httpError(status: Int)

    var errorDescription: String? {
        switch self {
        case .notAuthenticated:
            return "Not authenticated. Please sign in."
        case .invalidURI:
            return "The provided AT-URI is not valid for this operation."
        case .unexpectedRecordType:
            return "The record is not the expected type."
        case .contentConversionFailed:
            return "Failed to convert content to the output format."
        case .pdsResolutionFailed:
            return "Could not resolve the repository's PDS."
        case .httpError(let status):
            return "HTTP error (status \(status))."
        }
    }
}

extension URLError {
    /// True for transient network errors worth retrying (timeouts, DNS,
    /// connection lost, cannot connect). False for permanent errors like
    /// bad URLs, cancelled requests, or authentication failures.
    var isTransient: Bool {
        switch code {
        case .timedOut, .cannotFindHost, .cannotConnectToHost,
                .networkConnectionLost, .dnsLookupFailed,
                .notConnectedToInternet, .resourceUnavailable,
                .secureConnectionFailed:
            return true
        default:
            return false
        }
    }
}
