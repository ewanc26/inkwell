//
//  LoginStateManager+Profile.swift
//  Inkwell
//

import Foundation

extension LoginStateManager {
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
}
