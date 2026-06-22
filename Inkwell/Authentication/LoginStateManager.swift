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

    // MARK: - Client Metadata

    /// The OAuth client metadata for Inkwell.
    ///
    /// The `clientId` URL must serve the `client-metadata.json` file
    /// found in the repo's `oauth/` directory. In production this is
    /// `https://inkwell.ewancroft.uk/client-metadata.json`.
    private var appCredentials: AppCredentials {
        AppCredentials(
            clientId: "https://inkwell.ewancroft.uk/client-metadata.json",
            clientPassword: "",
            scopes: ["atproto"],
            callbackURL: URL(string: "inkwell://callback")!
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

            print("[SignIn] resolved \(identity.handle) → DID \(identity.did), PDS \(pdsURL.absoluteString)")

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

            // 5. Create Authenticator in manual mode to trigger auth
            let config = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .manualOnly
            )
            let auth = Authenticator(config: config)

            print("[SignIn] starting ASWebAuthenticationSession…")
            try await auth.authenticate()
            print("[SignIn] OAuth flow completed")

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
                if let profile = try? await self.fetchProfile(did: identity.did) {
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                }
            }

            return true
        } catch {
            print("[SignIn] error: \(error.localizedDescription)")
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
            print("[RestoreSession] no stored session found")
            clearSession()
            return
        }

        // Quick expiry check — don't bother building the authenticator
        // if the access token is already expired and there's no refresh token.
        if !storedLogin.accessToken.valid && storedLogin.refreshToken == nil {
            print("[RestoreSession] access token expired, no refresh token")
            clearSession()
            return
        }

        do {
            // Resolve identity for fresh DID
            guard let identity = try? await resolver.resolveHandle(storedHandle) else {
                print("[RestoreSession] identity resolution failed")
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

            print("[RestoreSession] session restored for \(identity.handle)")

            Task { [weak self] in
                guard let self else { return }
                if let profile = try? await self.fetchProfile(did: identity.did) {
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                }
            }
        } catch {
            print("[RestoreSession] failed: \(error.localizedDescription)")
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
            throw LoginError.httpError(status: (response as? HTTPURLResponse)?.statusCode ?? 0)
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
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await URLSession.shared.data(for: request)

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw LoginError.httpError(status: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }

        return data
    }

    // MARK: - Profile

    /// Fetches a Bluesky profile for the given DID.
    func fetchProfile(did: String) async throws -> ProfileSnapshot {
        // Use public API for profile (no auth needed)
        guard let url = URL(string: "https://public.api.bsky.app/xrpc/app.bsky.actor.getProfile?actor=\(did)") else {
            throw URLError(.badURL)
        }
        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw LoginError.httpError(status: (response as? HTTPURLResponse)?.statusCode ?? 0)
        }

        struct ProfileResponse: Decodable {
            let displayName: String?
            let avatar: String?
        }
        let profile = try JSONDecoder().decode(ProfileResponse.self, from: data)
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
    private func listAllRecords(
        from did: String,
        collection: String,
        maximumCount: Int = 1_000
    ) async throws -> [RepositoryRecord] {
        let pdsURL = try await repositoryPDSURL(for: did)
        let isOwn = did == currentDID

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

            let data: Data
            if isOwn {
                data = try await authenticatedData(
                    path: "/xrpc/com.atproto.repo.listRecords",
                    queryItems: queryItems
                )
            } else {
                data = try await unauthenticatedData(
                    pdsURL: pdsURL,
                    path: "/xrpc/com.atproto.repo.listRecords",
                    queryItems: queryItems
                )
            }

            let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
            allRecords.append(contentsOf: page.records)
            cursor = page.cursor
        } while cursor != nil && allRecords.count < maximumCount

        return Array(allRecords.prefix(maximumCount))
    }

    // MARK: - Publications

    /// Fetches all of the user's publication records.
    func fetchPublications() async throws -> [SiteStandardLexicon.PublicationRecord] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)
        return records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
    }

    /// Fetches publications from any user's repository.
    func fetchPublications(fromDID did: String) async throws -> [PublicationEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)
        return records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
                .map { PublicationEntry(uri: record.uri, authorDID: did, record: $0) }
        }
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
        return records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
    }

    /// Fetches documents from any user's repository.
    func fetchDocuments(fromDID did: String) async throws -> [DocumentEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.DocumentRecord.type)
        return records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
                .map { DocumentEntry(uri: record.uri, authorDID: did, record: $0) }
        }
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
        provider: ContentProvider
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard currentDID != nil else { throw LoginError.notAuthenticated }

        guard let contentRecord = provider.fromMarkdown(markdown) else {
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

    /// Fetches the user's subscriptions.
    func fetchSubscriptions() async throws -> [SubscriptionEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(
            from: did, collection: SiteStandardLexicon.Graph.SubscriptionRecord.type
        )
        return records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.Graph.SubscriptionRecord.self) }
                .map {
                    let rkey = ATURI.parse(record.uri)?.recordKey ?? ""
                    return SubscriptionEntry(uri: record.uri, recordKey: rkey, record: $0)
                }
        }
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

    /// Fetches the user's recommends.
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

    /// Deletes a recommend record.
    func deleteRecommend(recordKey: String) async throws {
        try await deleteRecord(
            collection: SiteStandardLexicon.Graph.RecommendRecord.type,
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
            retrieveLogin: { try store.read() },
            storeLogin: { try store.write($0) },
            clearLogin: { try store.delete() }
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
