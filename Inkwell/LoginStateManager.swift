//
//  LoginStateManager.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import Foundation
import Observation
import ATProtoKit
import ATProtoBluesky
import ATIdentityTools

@MainActor
@Observable
final class LoginStateManager {
    // MARK: - Public State
    private(set) var isAuthenticated = false
    private(set) var currentHandle: String?
    private(set) var errorMessage: String?

    // MARK: - Internal Session
    private var config: ATProtocolConfiguration?
    private var atProto: ATProtoKit?
    private var bluesky: ATProtoBluesky?

    // MARK: - Authentication
    /// Resolves the handle to a DID, discovers the PDS endpoint, and logs in.
    /// - Parameters:
    ///   - handle: The full Bluesky handle (e.g. "alice.bsky.social").
    ///   - password: The account password.
    /// - Returns: `true` if authentication succeeded.
    func signIn(handle: String, password: String) async -> Bool {
        let trimmed = handle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty, !password.isEmpty else {
            errorMessage = "Enter a handle and password."
            resetState()
            return false
        }
        do {
            // 1️⃣ Resolve handle → DID using ATIdentityTools
            let resolver = HandleResolver()
            guard let did = try await resolver.resolve(handle: trimmed) else {
                errorMessage = "Unable to resolve DID for handle."
                resetState()
                return false
            }
            // 2️⃣ Derive a PDS URL from the handle's domain (simple heuristic).
            let domain = trimmed.components(separatedBy: "@").last ?? trimmed
            guard let pdsURL = URL(string: "https://\(domain)") else {
                errorMessage = "Invalid PDS URL derived from handle."
                resetState()
                return false
            }
            // 3️⃣ Configure ATProtoKit with the discovered PDS and authenticate.
            let cfg = ATProtocolConfiguration()
            cfg.baseURL = pdsURL
            try await cfg.authenticate(with: trimmed, password: password)
            // Store configuration and create helper objects.
            self.config = cfg
            self.atProto = ATProtoKit(sessionConfiguration: cfg)
            self.bluesky = ATProtoBluesky(atProtoKitInstance: atProto!)
            // Update public state.
            self.currentHandle = trimmed
            self.isAuthenticated = true
            self.errorMessage = nil
            return true
        } catch {
            errorMessage = error.localizedDescription
            resetState()
            return false
        }
    }

    /// Signs the user out, clearing any stored session.
    func signOut() {
        resetState()
    }

    // MARK: - Helper
    private func resetState() {
        self.isAuthenticated = false
        self.currentHandle = nil
        self.errorMessage = nil
        self.config = nil
        self.atProto = nil
        self.bluesky = nil
    }
}
