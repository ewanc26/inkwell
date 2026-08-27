import Foundation
import Observation
import ATProtoKit
import InkwellShared

struct OfflineMutationFlushOutcome: Sendable {
    let completedCount: Int
    let failedCount: Int
    let pendingCount: Int

    static let empty = OfflineMutationFlushOutcome(
        completedCount: 0,
        failedCount: 0,
        pendingCount: 0
    )
}

/**
 * Native authenticated transport for the shared, bounded mutation queue.
 *
 * KMP owns the durable entry format, account boundary, retention, and file
 * storage. This adapter only maps those entries onto the existing iOS AT
 * Protocol client once a connection is available.
 */
@MainActor
@Observable
final class OfflineMutationStore {
    static let shared = OfflineMutationStore()

    private let queue: OfflineSyncQueue
    private(set) var pendingCount = 0
    private(set) var isSyncing = false

    private init() {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        queue = CreateOfflineSyncQueue_iosKt.createOfflineSyncQueue(cacheDirPath: directory.path)
    }

    func refresh(accountDID: String?) async {
        let entries = (try? await queue.load()) ?? []
        guard let accountDID else {
            pendingCount = 0
            return
        }
        pendingCount = entries.count { $0.accountDid == accountDID }
    }

    func enqueue(
        accountDID: String,
        kind: SyncMutationKind,
        subjectURI: String,
        commentText: String? = nil,
        replyToURI: String? = nil
    ) async throws {
        let entry = SyncQueueEntry(
            id: UUID().uuidString,
            accountDid: accountDID,
            kind: kind,
            subjectUri: subjectURI,
            createdAtMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            commentText: commentText,
            replyToUri: replyToURI
        )
        try await queue.enqueue(entry: entry)
        await refresh(accountDID: accountDID)
    }

    /// Replays this signed-in account's saved changes in chronological order.
    /// Subscription and recommendation toggles are state-aware, so retrying a
    /// completed entry is a no-op rather than a duplicate graph record.
    func flush(loginStateManager: LoginStateManager) async -> OfflineMutationFlushOutcome {
        guard !isSyncing, let accountDID = loginStateManager.currentDID else {
            return .empty
        }
        isSyncing = true
        defer { isSyncing = false }

        let entries = ((try? await queue.load()) ?? []).filter { $0.accountDid == accountDID }
        guard !entries.isEmpty else {
            pendingCount = 0
            return .empty
        }

        var subscriptions: [SubscriptionEntry]?
        var recommends: [RecommendEntry]?
        var completedIDs = Set<String>()
        var failedIDs = Set<String>()

        for entry in entries {
            do {
                if entry.kind == .subscribe {
                    if subscriptions == nil {
                        subscriptions = try await loginStateManager.fetchSubscriptions()
                    }
                    let currentSubscriptions = subscriptions ?? []
                    if !currentSubscriptions.contains(where: { $0.record.publication == entry.subjectUri }) {
                        let reference = try await loginStateManager.createSubscription(publicationURI: entry.subjectUri)
                        guard let recordKey = parseAtUri(reference.recordURI)?.recordKey else {
                            throw OfflineMutationReplayError.missingRecordKey
                        }
                        subscriptions = currentSubscriptions + [
                            SubscriptionEntry(
                                uri: reference.recordURI,
                                recordKey: recordKey,
                                record: SiteStandardLexicon.Graph.SubscriptionRecord(
                                    publication: entry.subjectUri,
                                    createdAt: Date()
                                )
                            )
                        ]
                    }
                } else if entry.kind == .unsubscribe {
                    if subscriptions == nil {
                        subscriptions = try await loginStateManager.fetchSubscriptions()
                    }
                    let matches = subscriptions?.filter { $0.record.publication == entry.subjectUri } ?? []
                    for match in matches {
                        try await loginStateManager.deleteSubscription(recordKey: match.recordKey)
                    }
                    subscriptions?.removeAll { $0.record.publication == entry.subjectUri }
                } else if entry.kind == .recommend {
                    if recommends == nil {
                        recommends = try await loginStateManager.fetchRecommends()
                    }
                    let currentRecommends = recommends ?? []
                    if !currentRecommends.contains(where: { $0.record.document == entry.subjectUri }) {
                        let reference = try await loginStateManager.createRecommend(documentURI: entry.subjectUri)
                        guard let recordKey = parseAtUri(reference.recordURI)?.recordKey else {
                            throw OfflineMutationReplayError.missingRecordKey
                        }
                        recommends = currentRecommends + [
                            RecommendEntry(
                                uri: reference.recordURI,
                                recordKey: recordKey,
                                record: SiteStandardLexicon.Graph.RecommendRecord(
                                    document: entry.subjectUri,
                                    createdAt: Date()
                                )
                            )
                        ]
                    }
                } else if entry.kind == .unrecommend {
                    if recommends == nil {
                        recommends = try await loginStateManager.fetchRecommends()
                    }
                    let matches = recommends?.filter { $0.record.document == entry.subjectUri } ?? []
                    for match in matches {
                        try await loginStateManager.deleteRecommend(recordKey: match.recordKey)
                    }
                    recommends?.removeAll { $0.record.document == entry.subjectUri }
                } else if entry.kind == .createcomment {
                    guard let commentText = entry.commentText else {
                        throw OfflineMutationReplayError.missingCommentText
                    }
                    try await loginStateManager.createComment(
                        subject: entry.subjectUri,
                        plaintext: commentText,
                        replyTo: entry.replyToUri,
                        onPage: nil
                    )
                }
                completedIDs.insert(entry.id)
            } catch is CancellationError {
                if !completedIDs.isEmpty {
                    try? await queue.remove(ids: completedIDs)
                }
                await refresh(accountDID: accountDID)
                return OfflineMutationFlushOutcome(
                    completedCount: completedIDs.count,
                    failedCount: failedIDs.count,
                    pendingCount: pendingCount
                )
            } catch {
                failedIDs.insert(entry.id)
                print("[OfflineMutationStore] Could not replay \(entry.kind): \(error)")
            }
        }

        if !completedIDs.isEmpty {
            try? await queue.remove(ids: completedIDs)
        }
        await refresh(accountDID: accountDID)
        return OfflineMutationFlushOutcome(
            completedCount: completedIDs.count,
            failedCount: failedIDs.count,
            pendingCount: pendingCount
        )
    }
}

private enum OfflineMutationReplayError: LocalizedError {
    case missingRecordKey
    case missingCommentText

    var errorDescription: String? {
        switch self {
        case .missingRecordKey: "The server did not return a record key."
        case .missingCommentText: "The saved comment was incomplete."
        }
    }
}
