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

    // MARK: - Storage
    @ObservationIgnored private let defaults: UserDefaults
    private let storedHandleKey = "storedAccountHandle"

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

                    let cfg = ATProtocolConfiguration(pdsURL: pdsURL.absoluteString)
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

    /// Signs the user out, clearing any stored session.
    func signOut() {
        clearSession(clearStoredAccount: true)
        errorMessage = nil
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
        }
    }
}
