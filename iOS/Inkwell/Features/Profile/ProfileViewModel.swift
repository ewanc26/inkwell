//
//  ProfileViewModel.swift
//  Inkwell
//

import Foundation
import Observation

@MainActor
@Observable
final class ProfileViewModel {
    private(set) var profile: BSkyActorProfile?
    private(set) var isLoading = false
    private(set) var errorMessage: String?

    func load(did: String) async {
        guard did.hasPrefix("did:") else {
            profile = nil
            errorMessage = "This account identifier is invalid."
            return
        }

        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            profile = try await BSkyProfileFetcher.fetchProfile(did: did)
        } catch is CancellationError {
            return
        } catch {
            errorMessage = "Couldn't load this profile: \(error.localizedDescription)"
        }
    }
}
