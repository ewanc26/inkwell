//
//  LoginStateManager.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import Foundation
import Observation
import ATProtoKit

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

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
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

    // MARK: - Helper
    private func clearSession(clearStoredAccount: Bool = false) {
        self.isAuthenticated = false
        self.currentHandle = nil
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
