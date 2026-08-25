//
//  MutedBlockedViewModel.swift
//  Inkwell
//

import Foundation
import Observation

@MainActor
@Observable
final class MutedBlockedViewModel {
    private let loginStateManager: LoginStateManager

    var isLoading = true
    var mutedActors: [ModeratedActor] = []
    var blockedActors: [BlockedActorEntry] = []
    var errorMessage: String?
    var removingKeys: Set<String> = []

    init(loginStateManager: LoginStateManager) {
        self.loginStateManager = loginStateManager
    }

    func load() async {
        isLoading = true
        errorMessage = nil
        do {
            let mutes = try await loginStateManager.fetchMutedActors()
            let blocks = try await loginStateManager.fetchBlockedActors()
            mutedActors = mutes
            blockedActors = blocks
        } catch {
            errorMessage = error.localizedDescription.isEmpty ? "Failed to load." : error.localizedDescription
        }
        isLoading = false
    }

    func unmute(did: String) async {
        guard !removingKeys.contains(did) else { return }
        removingKeys.insert(did)
        defer { removingKeys.remove(did) }

        do {
            try await loginStateManager.unmuteActor(did: did)
            mutedActors.removeAll { $0.did == did }
        } catch {
            errorMessage = error.localizedDescription.isEmpty ? "Failed to unmute." : error.localizedDescription
        }
    }

    func unblock(_ entry: BlockedActorEntry) async {
        guard !removingKeys.contains(entry.recordKey) else { return }
        removingKeys.insert(entry.recordKey)
        defer { removingKeys.remove(entry.recordKey) }

        do {
            try await loginStateManager.deleteBlock(recordKey: entry.recordKey)
            blockedActors.removeAll { $0.recordKey == entry.recordKey }
        } catch {
            errorMessage = error.localizedDescription.isEmpty ? "Failed to unblock." : error.localizedDescription
        }
    }
}
