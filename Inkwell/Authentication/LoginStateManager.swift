//
//  LoginStateManager.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import Foundation
import Observation
import ATProtoKit

nonisolated struct RepositoryRecord: Decodable, Sendable {
    let uri: String
    let cid: String?
    let value: UnknownType?
}

nonisolated struct TolerantRecordPage: Decodable, Sendable {
    let cursor: String?
    let records: [RepositoryRecord]

    private enum CodingKeys: String, CodingKey {
        case cursor
        case records
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        cursor = try container.decodeIfPresent(String.self, forKey: .cursor)
        records = try container.decode(LossyRecordArray.self, forKey: .records).elements
    }
}

private nonisolated struct LossyRecordArray: Decodable, Sendable {
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

/// Minimal identity record returned by Slingshot's `resolveMiniDoc` endpoint.
/// Mirrors the shape used by the website's `@ewanc26/atproto` package
/// (`ResolvedIdentity { did, pds }`), with `handle` included for convenience.
private struct SlingshotMiniDoc: Decodable {
    let did: String
    let handle: String
    let pds: String
}

/// Resolves a handle (or DID) straight to its DID + PDS endpoint via
/// Slingshot (microcosm.blue), in a single request. This replaces doing
/// our own DNS/.well-known handle resolution followed by a separate PLC
/// directory lookup — both of which can be slow or flaky in the iOS
/// Simulator's network stack. Slingshot is purpose-built for exactly this
/// "resolve a login handle fast" use case and already does its own caching.
private func resolveIdentityViaSlingshot(identifier: String) async throws -> SlingshotMiniDoc {
    var components = URLComponents(string: "https://slingshot.microcosm.blue/xrpc/com.bad-example.identity.resolveMiniDoc")!
    components.queryItems = [URLQueryItem(name: "identifier", value: identifier)]

    guard let url = components.url else {
        throw URLError(.badURL)
    }

    let sessionConfig = URLSessionConfiguration.ephemeral
    sessionConfig.timeoutIntervalForRequest = 8
    sessionConfig.timeoutIntervalForResource = 8
    let session = URLSession(configuration: sessionConfig)

    let (data, response) = try await session.data(from: url)
    guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
        throw URLError(.badServerResponse)
    }

    return try JSONDecoder().decode(SlingshotMiniDoc.self, from: data)
}

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
    /// authenticated session on launch. The root view should show a neutral
    /// loading state for this rather than flashing the login screen.
    private(set) var isRestoringSession = true

    // MARK: - Storage
    @ObservationIgnored private let defaults: UserDefaults
    private let storedHandleKey = "storedAccountHandle"
    private let storedPDSKey = "storedAccountPDS"

    /// A fixed identifier for the Keychain-backed session storage.
    ///
    /// `AppleSecureKeychain` (ATProtoKit's default `SecureKeychainProtocol`)
    /// namespaces its Keychain entries by `identifier.uuidString`, and that
    /// identifier defaults to a *freshly generated* `UUID()` on every
    /// `AppleSecureKeychain()` call. If we let it use the default, every
    /// app launch (and therefore every rebuild) writes its refresh token and
    /// password under a brand-new, never-seen-again Keychain key — so even
    /// though the values are sitting in the Keychain, nothing can find them
    /// again afterwards. Using one hardcoded identifier instead means we
    /// always read and write the same Keychain entry, so it survives
    /// rebuilds/relaunches exactly like any other persisted Keychain item.
    private static let sharedKeychainIdentifier = UUID(uuidString: "8E3F2D1A-9B47-4E6C-AF2D-5C19D7B4E021")!

    // MARK: - Internal Session
    private var config: ATProtocolConfiguration?
    private var atProto: ATProtoKit?
    private var repositoryClients: [String: ATProtoKit] = [:]
    private var repositoryPDSURLs: [String: URL] = [:]

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        // Record-type registration (site.standard.* plus every content
        // format) happens once, centrally, in InkwellApp's `.task` — see
        // `SiteStandardRegistration.swift` and `ContentFormatRegistration.swift`.
        // It used to be duplicated here against the old, now-removed
        // StandardPublication/StandardDocument types, which raced the
        // SiteStandardLexicon registration for the same `$type` keys
        // depending on which ran first.
    }

    // MARK: - Authentication
    /// Resolves the handle to a DID, discovers the PDS endpoint, and logs in.
    /// - Parameters:
    ///   - handle: The full Bluesky handle (e.g. "alice.bsky.social").
    ///   - password: The account password.
    /// - Returns: `true` if authentication succeeded.
    func signIn(handle: String, password: String) async -> Bool {
        let trimmed = handle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !password.isEmpty else {
            clearSession()
            errorMessage = "Enter a handle and password."
            return false
        }

        let timeoutSeconds: UInt64 = 15

        // Race authentication against a timeout.
        // group.addTask closures are nonisolated, so all @MainActor state
        // must be accessed via `await MainActor.run { … }`.
        return await withTaskGroup(of: Bool.self) { group in
            // Authentication task
            group.addTask {
                do {
                    // Resolve handle -> DID + PDS in a single request via Slingshot,
                    // rather than doing our own DNS/.well-known + PLC directory
                    // lookups (slow/unreliable in the Simulator, and the website
                    // hits the same problem class server-side, hence the shared
                    // approach via @ewanc26/atproto's resolveIdentity).
                    print("[SignIn] \(Date()) resolving identity via Slingshot for \(trimmed)")
                    let identity = try await resolveIdentityViaSlingshot(identifier: trimmed)
                    print("[SignIn] \(Date()) resolved DID: \(identity.did), PDS: \(identity.pds)")

                    guard let pdsURL = URL(string: identity.pds) else {
                        await MainActor.run {
                            self.clearSession()
                            self.errorMessage = "Could not determine the account's PDS."
                        }
                        return false
                    }

                    // Use the fixed Keychain identifier (see
                    // `sharedKeychainIdentifier`) so the tokens this saves can
                    // actually be found again by `restoreSessionIfPossible()`
                    // on the next launch.
                    let cfg = await ATProtocolConfiguration(
                        pdsURL: pdsURL.absoluteString,
                        keychainProtocol: AppleSecureKeychain(identifier: LoginStateManager.sharedKeychainIdentifier)
                    )
                    print("[SignIn] \(Date()) authenticating against PDS")
                    try await cfg.authenticate(with: trimmed, password: password)
                    print("[SignIn] \(Date()) authenticated successfully")

                    let proto = await ATProtoKit(sessionConfiguration: cfg)
                    print("[SignIn] \(Date()) ATProtoKit session initialised")

                    await MainActor.run {
                        self.config = cfg
                        self.atProto = proto
                        self.currentHandle = trimmed
                        self.currentDID = identity.did
                        self.isAuthenticated = true
                        self.errorMessage = nil
                        self.defaults.set(trimmed, forKey: self.storedHandleKey)
                        // Persisted alongside the handle so a relaunch can
                        // rebuild the same ATProtocolConfiguration(pdsURL:)
                        // without re-resolving it via Slingshot.
                        self.defaults.set(identity.pds, forKey: self.storedPDSKey)
                    }

                    // Best-effort, non-blocking: don't hold up sign-in completion
                    // (or the timeout race above) on a second network round-trip.
                    // Uses ATProtoKit's own getProfile(for:) rather than a hand-rolled
                    // request, so it picks up everything the AppView knows about the
                    // account (display name, avatar, etc.) in one call.
                    Task {
                        do {
                            let profile = try await proto.getProfile(for: identity.did)
                            let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                            await MainActor.run {
                                self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                                self.avatarURL = profile.avatarImageURL
                            }
                        } catch {
                            // Profile metadata is cosmetic — leave displayName/avatarURL
                            // as nil and let the UI fall back accordingly.
                        }
                    }

                    return true
                } catch {
                    let message = error.localizedDescription
                    await MainActor.run {
                        self.clearSession()
                        self.errorMessage = message
                    }
                    return false
                }
            }

            // Timeout task
            group.addTask {
                do {
                    try await Task.sleep(nanoseconds: timeoutSeconds * 1_000_000_000)
                } catch {
                    // Cancelled because the auth task already finished first.
                    // Don't touch state — it's already been set by that task.
                    return false
                }

                await MainActor.run {
                    self.clearSession()
                    self.errorMessage = "Login timed out. Please check your internet connection and try again."
                }
                return false
            }

            // Take whichever finishes first, cancel the other
            let first = await group.next() ?? false
            group.cancelAll()
            return first
        }
    }

    /// Attempts to silently resume a previously authenticated session using
    /// the refresh token already sitting in the Keychain, without requiring
    /// the handle/password again.
    ///
    /// This is what actually fixes "signing in every rebuild": as long as
    /// the Keychain item written by `signIn(handle:password:)` survives the
    /// rebuild (which it does for a normal incremental Xcode run — only a
    /// full app deletion or simulator erase clears it), this exchanges the
    /// stored refresh token for a fresh access token and restores the
    /// session, completely offline-of-password.
    ///
    /// Call this once on launch (e.g. from a `.task` on the root view).
    /// Safe to call even if there's nothing to restore — it just falls
    /// through to the login screen.
    func restoreSessionIfPossible() async {
        defer { isRestoringSession = false }

        guard let storedHandle = defaults.string(forKey: storedHandleKey),
              let storedPDS = defaults.string(forKey: storedPDSKey) else {
            // Nothing was ever persisted (first launch, or a build from
            // before this restore logic existed) — nothing to restore.
            return
        }

        let cfg = ATProtocolConfiguration(
            pdsURL: storedPDS,
            keychainProtocol: AppleSecureKeychain(identifier: LoginStateManager.sharedKeychainIdentifier)
        )

        do {
            print("[RestoreSession] \(Date()) attempting to resume session for \(storedHandle)")

            // Reads the refresh token straight out of the Keychain (via the
            // fixed identifier above) and exchanges it for a new access
            // token. No password and no Slingshot round-trip required.
            //
            // Known limitation: if the refresh token itself is within ~10
            // seconds of expiring at the exact moment of a cold launch,
            // ATProtoKit's refreshSession() tries to fall back to a
            // password re-auth keyed off an in-memory UserSessionRegistry
            // entry that won't exist yet on a fresh process, and throws
            // instead. That's a narrow race in ATProtoKit itself; on
            // failure we simply fall back to the login screen below rather
            // than crash.
            try await cfg.refreshSession()

            let proto = await ATProtoKit(sessionConfiguration: cfg)

            guard let session = try await proto.getUserSession() else {
                print("[RestoreSession] \(Date()) refresh succeeded but no session was registered")
                clearSession()
                return
            }

            self.config = cfg
            self.atProto = proto
            self.currentHandle = session.handle
            self.currentDID = session.sessionDID
            self.isAuthenticated = true
            self.errorMessage = nil
            print("[RestoreSession] \(Date()) session restored for \(session.handle)")

            Task {
                do {
                    let profile = try await proto.getProfile(for: session.sessionDID)
                    let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                    await MainActor.run {
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarImageURL
                    }
                } catch {
                    // Cosmetic only — same as in signIn(handle:password:).
                }
            }
        } catch {
            // Refresh token missing, expired beyond recovery, or no network.
            // Fall back to the login screen rather than getting stuck.
            // Deliberately doesn't clear the stored handle/PDS here, since a
            // transient network failure shouldn't force a fresh sign-in —
            // only an explicit signOut() does that.
            print("[RestoreSession] \(Date()) failed to restore session: \(error.localizedDescription)")
            clearSession()
        }
    }

    /// Signs the user out, clearing any stored session.
    func signOut() {
        let configToRevoke = config
        let keychainIdentifier = LoginStateManager.sharedKeychainIdentifier

        clearSession(clearStoredAccount: true)
        errorMessage = nil

        Task {
            // Best-effort cleanup, run after local state is already cleared
            // above so the UI doesn't wait on it. Without this, the refresh
            // token/password would still sit in the Keychain under the fixed
            // identifier and restoreSessionIfPossible() would silently log
            // the account back in on the next launch — defeating "sign out".
            try? await configToRevoke?.deleteSession()
            let keychain = AppleSecureKeychain(identifier: keychainIdentifier)
            try? await keychain.deleteRefreshToken()
            try? await keychain.deletePassword()
        }
    }

    // MARK: - standard.site Records API

    /// Fetches all standard.site publication records from the user's repository.
    func fetchPublications() async throws -> [SiteStandardLexicon.PublicationRecord] {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }
        _ = atProto
        let records = try await listAllRecords(from: session.sessionDID, collection: SiteStandardLexicon.PublicationRecord.type)

        var publications: [SiteStandardLexicon.PublicationRecord] = []
        for record in records {
            if let value = record.value, let pub = value.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) {
                publications.append(pub)
            }
        }
        return publications
    }

    /// Fetches all standard.site document records from the user's repository.
    func fetchDocuments() async throws -> [SiteStandardLexicon.DocumentRecord] {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }
        _ = atProto
        let records = try await listAllRecords(from: session.sessionDID, collection: SiteStandardLexicon.DocumentRecord.type)

        var documents: [SiteStandardLexicon.DocumentRecord] = []
        for record in records {
            if let value = record.value, let doc = value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) {
                documents.append(doc)
            }
        }
        return documents
    }

    /// Downloads raw bytes of a blob by its CID.
    func downloadBlob(cid: String) async throws -> Data {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }
        return try await atProto.getBlob(from: session.sessionDID, cid: cid)
    }

    /// Downloads a blob from the PDS hosting the specified repository.
    func downloadBlob(cid: String, fromDID did: String) async throws -> Data {
        let client = try await repositoryClient(for: did)
        return try await client.getBlob(from: did, cid: cid)
    }

    /// Creates and publishes a new standard.site document record.
    ///
    /// - Parameters:
    ///   - title: The document title.
    ///   - description: Optional description.
    ///   - path: Optional URL path segment.
    ///   - site: The publication URL this document belongs to.
    ///   - markdown: The markdown content to convert and store.
    ///   - provider: The content provider that converts markdown to the
    ///     format-specific AT Protocol record.
    @discardableResult
    func createDocument(
        title: String,
        description: String?,
        path: String?,
        site: String,
        markdown: String,
        provider: ContentProvider
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        // Convert markdown to the format-specific content record.
        guard let contentRecord = provider.fromMarkdown(markdown) else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "Failed to convert markdown to \(provider.label) format"])
        }

        // Build the standard.site document wrapper. publishedAt is a `Date`
        // here, not a pre-formatted ISO8601 string — DocumentRecord's own
        // `encode(to:)` handles AT Protocol datetime formatting.
        guard (ATURI.parse(site)?.collection == SiteStandardLexicon.PublicationRecord.type) ||
                (URL(string: site)?.scheme?.lowercased() == "https") else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "The document site must be a publication AT-URI or HTTPS URL."])
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

        // Wrap in UnknownType and create the record. shouldValidate is false
        // because site.standard.document is not a Bluesky lexicon — the PDS
        // would reject it if validation were enabled.
        let record = UnknownType.record(document)
        return try await atProto.createRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.document",
            shouldValidate: false,
            record: record
        )
    }

    // MARK: - Subscription API

    /// Creates a `site.standard.graph.subscription` record, subscribing the
    /// user to the publication at the given AT-URI.
    ///
    /// - Parameter publicationURI: The AT-URI of the publication record
    ///   (e.g. `at://did:plc:abc123/site.standard.publication/xyz789`).
    /// - Returns: The strong reference (URI + CID) of the created subscription.
    @discardableResult
    func createSubscription(publicationURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        guard ATURI.parse(publicationURI)?.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "Subscriptions must reference a Standard.site publication AT-URI."])
        }

        let subscription = SiteStandardLexicon.Graph.SubscriptionRecord(
            publication: publicationURI,
            createdAt: Date()
        )
        let record = UnknownType.record(subscription)
        return try await atProto.createRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.graph.subscription",
            shouldValidate: false,
            record: record
        )
    }

    /// Fetches all of the user's subscription records.
    ///
    /// Each entry includes the subscription record itself plus the AT-URI and
    /// record key (needed to unsubscribe).
    func fetchSubscriptions() async throws -> [SubscriptionEntry] {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        _ = atProto
        let records = try await listAllRecords(
            from: session.sessionDID,
            collection: SiteStandardLexicon.Graph.SubscriptionRecord.type
        )

        var entries: [SubscriptionEntry] = []
        for record in records {
            if let value = record.value,
               let sub = value.getRecord(ofType: SiteStandardLexicon.Graph.SubscriptionRecord.self) {
                let rkey = ATURI.parse(record.uri)?.recordKey ?? ""
                entries.append(SubscriptionEntry(
                    uri: record.uri,
                    recordKey: rkey,
                    record: sub
                ))
            }
        }
        return entries
    }

    /// Deletes a subscription record (unsubscribes from a publication).
    ///
    /// - Parameter recordKey: The record key of the subscription to delete.
    func deleteSubscription(recordKey: String) async throws {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        try await atProto.deleteRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.graph.subscription",
            recordKey: recordKey
        )
    }

    // MARK: - Cross-repo Record Fetching

    /// Fetches publication records from any user's repository (not just the
    /// current user's). Each entry includes the AT-URI and author DID alongside
    /// the decoded record.
    ///
    /// - Parameter did: The DID of the repository to fetch from.
    func fetchPublications(fromDID did: String) async throws -> [PublicationEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)

        var entries: [PublicationEntry] = []
        for record in records {
            if let value = record.value,
               let pub = value.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) {
                entries.append(PublicationEntry(
                    uri: record.uri,
                    authorDID: did,
                    record: pub
                ))
            }
        }
        return entries
    }

    /// Fetches document records from any user's repository.
    ///
    /// - Parameter did: The DID of the repository to fetch from.
    func fetchDocuments(fromDID did: String) async throws -> [DocumentEntry] {
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.DocumentRecord.type)

        var entries: [DocumentEntry] = []
        for record in records {
            if let value = record.value,
               let doc = value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) {
                entries.append(DocumentEntry(
                    uri: record.uri,
                    authorDID: did,
                    record: doc
                ))
            }
        }
        return entries
    }

    /// Fetches and decodes one Standard.site publication from its AT-URI.
    func fetchPublication(uri: String) async throws -> PublicationEntry {
        guard let parsed = ATURI.parse(uri),
              parsed.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "Invalid publication AT-URI."])
        }
        let client = try await repositoryClient(for: parsed.did)
        let output = try await client.getRepositoryRecord(
            from: parsed.did,
            collection: parsed.collection,
            recordKey: parsed.recordKey
        )
        guard let value = output.value,
              let publication = value.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "The record isn't a Standard.site publication."])
        }
        return PublicationEntry(uri: output.uri, authorDID: parsed.did, record: publication)
    }

    /// Fetches and decodes one Standard.site document from its AT-URI.
    func fetchDocument(uri: String) async throws -> DocumentEntry {
        guard let parsed = ATURI.parse(uri),
              parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "Invalid document AT-URI."])
        }
        let client = try await repositoryClient(for: parsed.did)
        let output = try await client.getRepositoryRecord(
            from: parsed.did,
            collection: parsed.collection,
            recordKey: parsed.recordKey
        )
        guard let value = output.value,
              let document = value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "The record isn't a Standard.site document."])
        }
        return DocumentEntry(uri: output.uri, authorDID: parsed.did, record: document)
    }

    private func repositoryClient(for did: String) async throws -> ATProtoKit {
        if did == currentDID, let atProto {
            return atProto
        }
        if let cached = repositoryClients[did] {
            return cached
        }

        let pdsURL = try await repositoryPDSURL(for: did)
        let remoteConfiguration = ATProtocolConfiguration(pdsURL: pdsURL.absoluteString)
        let client = await ATProtoKit(sessionConfiguration: remoteConfiguration)
        repositoryClients[did] = client
        return client
    }

    private func repositoryPDSURL(for did: String) async throws -> URL {
        if let cached = repositoryPDSURLs[did] {
            return cached
        }
        if did == currentDID,
           let storedPDS = defaults.string(forKey: storedPDSKey),
           let url = URL(string: storedPDS) {
            repositoryPDSURLs[did] = url
            return url
        }

        let identity = try await resolveIdentityViaSlingshot(identifier: did)
        guard identity.did == did, let url = URL(string: identity.pds) else {
            throw NSError(domain: "LoginStateManager", code: 404, userInfo: [NSLocalizedDescriptionKey: "Could not resolve the repository's PDS."])
        }
        repositoryPDSURLs[did] = url
        return url
    }

    private func listAllRecords(
        from did: String,
        collection: String,
        maximumCount: Int = 1_000
    ) async throws -> [RepositoryRecord] {
        let pdsURL = try await repositoryPDSURL(for: did)
        var records: [RepositoryRecord] = []
        var cursor: String?

        repeat {
            var components = URLComponents(
                url: pdsURL.appending(path: "xrpc/com.atproto.repo.listRecords"),
                resolvingAgainstBaseURL: false
            )
            var queryItems = [
                URLQueryItem(name: "repo", value: did),
                URLQueryItem(name: "collection", value: collection),
                URLQueryItem(name: "limit", value: String(min(100, maximumCount - records.count)))
            ]
            if let cursor {
                queryItems.append(URLQueryItem(name: "cursor", value: cursor))
            }
            components?.queryItems = queryItems
            guard let url = components?.url else { throw URLError(.badURL) }

            let (data, response) = try await URLSession.shared.data(from: url)
            guard let http = response as? HTTPURLResponse,
                  (200...299).contains(http.statusCode) else {
                throw URLError(.badServerResponse)
            }
            let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
            records.append(contentsOf: page.records)
            cursor = page.cursor
        } while cursor != nil && records.count < maximumCount

        return Array(records.prefix(maximumCount))
    }

    /// Fetches publications from the current user's repository, enriched with
    /// their AT-URIs and author DID.
    func fetchPublicationsWithURIs() async throws -> [PublicationEntry] {
        guard let session = try await atProto?.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }
        return try await fetchPublications(fromDID: session.sessionDID)
    }

    /// Fetches documents from the current user's repository, enriched with
    /// their AT-URIs and author DID.
    func fetchDocumentsWithURIs() async throws -> [DocumentEntry] {
        guard let session = try await atProto?.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }
        return try await fetchDocuments(fromDID: session.sessionDID)
    }

    // MARK: - Publication Creation

    /// Creates a `site.standard.publication` record in the user's repository.
    ///
    /// - Parameters:
    ///   - url: The base URL for the publication (e.g. `https://mysite.com`).
    ///   - name: The display name of the publication.
    ///   - description: Optional description.
    /// - Returns: The strong reference (URI + CID) of the created publication.
    @discardableResult
    func createPublication(
        url: String,
        name: String,
        description: String?
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        let publication = SiteStandardLexicon.PublicationRecord(
            url: url,
            name: name,
            description: description
        )
        let record = UnknownType.record(publication)
        return try await atProto.createRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.publication",
            shouldValidate: false,
            record: record
        )
    }

    // MARK: - Recommend API

    /// Creates a `site.standard.graph.recommend` record, endorsing a document.
    ///
    /// - Parameter documentURI: The AT-URI of the document record to recommend.
    /// - Returns: The strong reference of the created recommend.
    @discardableResult
    func createRecommend(documentURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        guard ATURI.parse(documentURI)?.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw NSError(domain: "LoginStateManager", code: 422, userInfo: [NSLocalizedDescriptionKey: "Recommends must reference a Standard.site document AT-URI."])
        }

        let recommend = SiteStandardLexicon.Graph.RecommendRecord(
            document: documentURI,
            createdAt: Date()
        )
        let record = UnknownType.record(recommend)
        return try await atProto.createRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.graph.recommend",
            shouldValidate: false,
            record: record
        )
    }

    /// Fetches all of the user's recommend records.
    func fetchRecommends() async throws -> [RecommendEntry] {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        _ = atProto
        let records = try await listAllRecords(
            from: session.sessionDID,
            collection: SiteStandardLexicon.Graph.RecommendRecord.type
        )

        var entries: [RecommendEntry] = []
        for record in records {
            if let value = record.value,
               let rec = value.getRecord(ofType: SiteStandardLexicon.Graph.RecommendRecord.self) {
                let rkey = ATURI.parse(record.uri)?.recordKey ?? ""
                entries.append(RecommendEntry(
                    uri: record.uri,
                    recordKey: rkey,
                    record: rec
                ))
            }
        }
        return entries
    }

    /// Deletes a recommend record, withdrawing an endorsement of a document.
    ///
    /// - Parameter recordKey: The record key of the recommend to delete.
    func deleteRecommend(recordKey: String) async throws {
        guard let atProto = atProto, let session = try await atProto.getUserSession() else {
            throw NSError(domain: "LoginStateManager", code: 401, userInfo: [NSLocalizedDescriptionKey: "User not authenticated"])
        }

        try await atProto.deleteRecord(
            repositoryDID: session.sessionDID,
            collection: "site.standard.graph.recommend",
            recordKey: recordKey
        )
    }

    // MARK: - Helper
    private func clearSession(clearStoredAccount: Bool = false) {
        self.isAuthenticated = false
        self.currentHandle = nil
        self.currentDID = nil
        self.displayName = nil
        self.avatarURL = nil
        self.config = nil
        self.atProto = nil

        if clearStoredAccount {
            defaults.removeObject(forKey: storedHandleKey)
            defaults.removeObject(forKey: storedPDSKey)
        }
    }
}
