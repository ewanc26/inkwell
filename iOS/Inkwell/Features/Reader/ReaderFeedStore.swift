//
//  ReaderFeedStore.swift
//  Inkwell
//
//  The reader feed's shared loaded state and fetch logic — see
//  BrowseDocumentsView.swift for the view that displays it.
//

import Foundation
import ATProtoKit
import InkwellShared

// MARK: - Pagination State

/// Tracks cursor-based pagination across multiple subscribed publications.
/// Each DID gets its own cursor; items are merged into a single feed sorted by
/// `publishedAt` descending.
@MainActor
struct FollowingFeedState {
    var items: [ReaderFeedItem] = []
    // Starts false so `loadData` can begin the initial request. The loading
    // flag is set synchronously by `loadFollowingFeed` once work is underway.
    var isLoading = false
    var isLoadingNextPage = false
    var error: String?
    /// Per-DID cursors for the next page of `site.standard.document`.
    var cursors: [String: String] = [:]
    var hasMorePages = true
    /// Whether the initial fetch has completed (even if empty).
    var hasLoaded = false
}

enum ReaderFeed: String, CaseIterable, Identifiable {
    case following = "Following"
    case yours = "Yours"

    var id: Self { self }
}

private enum ReaderFeedLoadError: LocalizedError {
    case subscriptionsTimedOut

    var errorDescription: String? {
        switch self {
        case .subscriptionsTimedOut:
            "Couldn't reach your subscriptions. Check your connection and try again."
        }
    }
}

/// Ensures only the first completion of an unstructured fetch race resumes its
/// continuation. NSLock keeps the small critical section safe without making
/// the Reader's @MainActor state responsible for a stalled network task.
private final class ReaderFeedRaceGate: @unchecked Sendable {
    private let lock = NSLock()
    private var completed = false

    func claim() -> Bool {
        lock.lock()
        defer { lock.unlock() }
        guard !completed else { return false }
        completed = true
        return true
    }
}

/// Owns the Reader feed's loaded state and fetch logic, shared across the
/// app so loading can start as soon as the user is authenticated —
/// independent of whether the Reader tab has actually been shown yet —
/// instead of only starting once `BrowseDocumentsView` first appears.
@MainActor
@Observable
final class ReaderFeedStore {
    static let shared = ReaderFeedStore()
    private init() {}

    var selectedFeed: ReaderFeed = .following
    var followingState = FollowingFeedState()
    var yours: [ReaderFeedItem] = []
    var isLoadingYours = false
    var yoursError: String?

    // MARK: - Jetstream + Cache

    private let jetstreamClient = createSharedJetstreamClient()
    private let feedCache = createSharedFeedCache()
    private var jetstreamTask: Task<Void, Never>?
    private var revealedModeratedItemIDs: Set<String> = []

    /// A filtered item remains visible as a neutral warning card until the
    /// reader explicitly reveals it for this app session.
    func moderationPresentation(for item: ReaderFeedItem) -> ContentModerationPresentation {
        revealedModeratedItemIDs.contains(item.id) ? .visible : item.moderationPresentation
    }

    func revealModeratedItem(_ item: ReaderFeedItem) {
        revealedModeratedItemIDs.insert(item.id)
    }

    func refreshModeration() {
        revealedModeratedItemIDs.removeAll()
    }

    // MARK: - Data Loading

    /// Fetches both feeds. Skipped if a load is already in flight or has
    /// already completed once, unless `force` is set (pull-to-refresh, the
    /// manual reload button) — callers that just want loading to have
    /// *started* (e.g. as soon as the user's authenticated) don't need to
    /// re-trigger it themselves once it already has.
    func loadData(loginStateManager: LoginStateManager, force: Bool = false) async {
        guard force || !(followingState.hasLoaded || followingState.isLoading) else { return }
        async let _following: () = loadFollowingFeed(loginStateManager: loginStateManager, force: force)
        async let _yours: () = loadYoursFeed(loginStateManager: loginStateManager)
        _ = await (_following, _yours)
    }

    // MARK: Following — paginated

    /// Per-publication fetch timeout — a single unreachable PDS must not
    /// block the entire feed from appearing.
    private static let publicationTimeout: Duration = .seconds(8)
    /// Subscription records are the gateway to the whole Following feed. They
    /// need their own bound: the per-publication timeout below cannot help if
    /// this request never completes.
    private static let subscriptionsTimeout: Duration = .seconds(12)
    private static let profileTimeout: Duration = .seconds(4)

    /// Fetches subscriptions and the first page of documents from each followed
    /// publication. Subsequent pages are loaded on-demand via the sentinel.
    ///
    /// Results are displayed progressively — the spinner disappears as soon as
    /// the first publication returns, and each subsequent publication's items
    /// are merged into the feed immediately.  A per-publication timeout ensures
    /// one slow or unreachable PDS cannot hold up the rest.
    private func loadFollowingFeed(loginStateManager: LoginStateManager, force: Bool) async {
        followingState.isLoading = true
        followingState.error = nil

        // 1. Show cached data immediately (if available).
        if !force, let cached = try? await feedCache.load(limit: 200), !cached.isEmpty {
            followingState.items = cached.compactMap { $0.toReaderFeedItem() }
            followingState.items = deduplicated(followingState.items)
            followingState.isLoading = false
            followingState.hasLoaded = true
        }

        do {
            let subscriptions = try await fetchSubscriptions(
                loginStateManager: loginStateManager,
                timeout: ReaderFeedStore.subscriptionsTimeout
            )

            // Reset state
            if force || followingState.items.isEmpty {
                followingState.items = []
            }
            followingState.cursors = [:]
            followingState.hasMorePages = !subscriptions.isEmpty

            guard !subscriptions.isEmpty else {
                followingState.isLoading = false
                followingState.hasLoaded = true
                return
            }

            // Collect unique DIDs for profile resolution.
            let uniqueDIDs = Set(subscriptions.compactMap { sub in
                parseAtUri(sub.record.publication)?.did
            })
            let profiles = await resolveProfiles(dids: uniqueDIDs)

            // 2. Fetch first page from each subscribed publication concurrently.
            //    Items appear progressively — the spinner disappears as soon as
            //    the first batch lands and each subsequent batch is merged in.
            await withTaskGroup(of: (did: String, items: [ReaderFeedItem], cursor: String?).self) { group in
                for subscription in subscriptions {
                    let pubURI = subscription.record.publication
                    guard let pubDID = parseAtUri(pubURI)?.did else { continue }
                    group.addTask { [pubURI, pubDID] in
                        // Race the actual fetch against a per-publication
                        // timeout so one slow PDS cannot block the whole feed.
                        await withTaskGroup(
                            of: (did: String, items: [ReaderFeedItem], cursor: String?).self
                        ) { inner in
                            inner.addTask {
                                let pubEntry = try? await loginStateManager.fetchPublication(uri: pubURI)
                                let (records, cursor) = (try? await loginStateManager.listRecordsPage(
                                    from: pubDID,
                                    collection: SiteStandardLexicon.DocumentRecord.type,
                                    limit: 25
                                )) ?? ([], nil)

                                let profile = profiles[pubDID] ?? profiles[pubDID.lowercased()]
                                let items: [ReaderFeedItem] = records.compactMap { record in
                                    guard let value = record.value,
                                          let doc = value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self),
                                          doc.site == pubURI else { return nil }
                                    return ReaderFeedItem(
                                        document: DocumentEntry(uri: record.uri, cid: record.cid, authorDID: pubDID, record: doc),
                                        publication: pubEntry,
                                        authorProfile: profile
                                    )
                                }
                                return (pubDID, items, cursor)
                            }
                            inner.addTask {
                                try? await Task.sleep(for: ReaderFeedStore.publicationTimeout)
                                return (pubDID, [], nil)
                            }
                            // Return whichever finishes first; the other is
                            // cancelled when the group scope exits.
                            var winner = (pubDID, [ReaderFeedItem](), String?.none)
                            for await r in inner {
                                winner = r
                                break
                            }
                            return winner
                        }
                    }
                }
                for await result in group {
                    guard !result.items.isEmpty else { continue }
                    followingState.items.append(contentsOf: result.items)
                    if let cursor = result.cursor {
                        followingState.cursors[result.did] = cursor
                    }
                    // Progressive: reveal the feed after the first batch
                    // arrives so the user sees content immediately rather
                    // than waiting for every PDS to respond.
                    if !followingState.hasLoaded {
                        followingState.isLoading = false
                        followingState.hasLoaded = true
                    }
                    followingState.items = deduplicated(followingState.items)
                }
            }

            // Ensure flags are finalised even if every publication timed out.
            followingState.isLoading = false
            followingState.hasLoaded = true

            // Check if any DID still has more pages.
            followingState.hasMorePages = !followingState.cursors.isEmpty

            // 3. Cache the results for next launch.
            let cachedItems = followingState.items.map { $0.toCachedFeedItem() }
            try? await feedCache.save(items: Array(cachedItems))

            // 4. Connect to Jetstream for live updates.
            startJetstreamSubscription(
                dids: Array(uniqueDIDs),
                loginStateManager: loginStateManager
            )

        } catch {
            followingState.error = error.localizedDescription
            followingState.isLoading = false
            followingState.hasLoaded = true
        }
    }

    // MARK: - Jetstream Live Updates

    /// Starts a Jetstream WebSocket subscription for the given DIDs.
    /// Events are parsed into CachedFeedItem objects and merged into the
    /// feed in real time.
    private func startJetstreamSubscription(
        dids: [String],
        loginStateManager: LoginStateManager
    ) {
        jetstreamTask?.cancel()
        guard !dids.isEmpty else { return }

        let config = createJetstreamConfig(dids: dids)
        let client = jetstreamClient
        let cache = feedCache

        jetstreamTask = Task { [weak self] in
            for await payload in streamJetstreamPayloads(client: client, config: config) {
                guard !Task.isCancelled else { break }
                guard payload.collection == "site.standard.document" else { continue }

                // Parse the event into a CachedFeedItem.
                let cachedItem = payload.toCachedFeedItem()
                let publication: PublicationEntry?
                if let cachedItem {
                    // Jetstream document commits contain the document but
                    // not its publication record. Resolve that record once
                    // here so a live card uses the same publication theme
                    // as the document it opens.
                    publication = try? await loginStateManager.fetchPublication(uri: cachedItem.site)
                } else {
                    publication = nil
                }

                await MainActor.run {
                    guard let self else { return }

                    if let cachedItem {
                        // Convert to a ReaderFeedItem and merge into the feed.
                        let newItem = cachedItem.toReaderFeedItem(
                            publication: publication,
                            isCached: false
                        )
                        if let index = self.followingState.items.firstIndex(where: { $0.id == newItem.id }) {
                            // Enrich an already-visible cached card once its
                            // publication record arrives, rather than leaving
                            // it on the fallback system theme.
                            if self.followingState.items[index].publication == nil,
                               newItem.publication != nil {
                                self.followingState.items[index] = newItem
                            }
                        } else {
                            self.followingState.items.append(newItem)
                        }
                        self.followingState.items = self.deduplicated(self.followingState.items)
                    } else if payload.operation == "delete" {
                        // Handle deletes by removing the item from the feed.
                        let deletedUri = "at://\(payload.did)/\(payload.collection)/\(payload.rkey)"
                        self.followingState.items.removeAll { $0.id == deletedUri }
                    }
                }

                // Persist to cache periodically (every event for now;
                // could batch for efficiency).
                if let cachedItem {
                    try? await cache.upsert(items: [cachedItem])
                } else if payload.operation == "delete" {
                    let deletedUri = "at://\(payload.did)/\(payload.collection)/\(payload.rkey)"
                    try? await cache.remove(uri: deletedUri)
                }
            }
        }
    }

    /// Stops the Jetstream subscription (e.g. on logout).
    func stopJetstream() {
        jetstreamTask?.cancel()
        jetstreamTask = nil
        Task { try? await jetstreamClient.disconnect() }
    }

    /// Resolves Bluesky profiles for a set of DIDs concurrently.
    private func resolveProfiles(dids: Set<String>) async -> [String: BSkyActorProfile] {
        await withTaskGroup(of: (String, BSkyActorProfile?).self) { group in
            for did in dids {
                group.addTask {
                    (did, await self.fetchProfile(did: did))
                }
            }
            var result: [String: BSkyActorProfile] = [:]
            for await (did, profile) in group {
                if let profile = profile {
                    result[did] = profile
                    result[profile.handle.lowercased()] = profile
                }
            }
            return result
        }
    }

    /// Races the authenticated subscription fetch against a short timeout.
    /// Without this, a stalled PDS leaves the Reader spinner on screen before
    /// the per-publication timeouts have a chance to take effect.
    private func fetchSubscriptions(
        loginStateManager: LoginStateManager,
        timeout: Duration
    ) async throws -> [SubscriptionEntry] {
        let fetchTask = Task { try await loginStateManager.fetchSubscriptions() }
        return try await withCheckedThrowingContinuation { continuation in
            let gate = ReaderFeedRaceGate()

            Task {
                do {
                    let subscriptions = try await fetchTask.value
                    if gate.claim() {
                        continuation.resume(returning: subscriptions)
                    }
                } catch {
                    if gate.claim() {
                        continuation.resume(throwing: error)
                    }
                }
            }

            Task {
                try? await Task.sleep(for: timeout)
                guard gate.claim() else { return }
                fetchTask.cancel()
                continuation.resume(throwing: ReaderFeedLoadError.subscriptionsTimedOut)
            }
        }
    }

    /// Profile data is decorative. A hung public-profile request must never
    /// delay publication cards from becoming readable.
    private func fetchProfile(did: String) async -> BSkyActorProfile? {
        let fetchTask = Task { try? await BSkyProfileFetcher.fetchProfile(did: did) }
        return await withCheckedContinuation { continuation in
            let gate = ReaderFeedRaceGate()

            Task {
                let profile = await fetchTask.value
                if gate.claim() {
                    continuation.resume(returning: profile)
                }
            }

            Task {
                try? await Task.sleep(for: ReaderFeedStore.profileTimeout)
                guard gate.claim() else { return }
                fetchTask.cancel()
                continuation.resume(returning: nil)
            }
        }
    }

    /// Loads the next page for each subscribed publication that still has a
    /// cursor, merging the results into the feed.
    func loadNextFollowingPage(loginStateManager: LoginStateManager) async {
        guard !followingState.isLoadingNextPage, followingState.hasMorePages else { return }
        followingState.isLoadingNextPage = true
        followingState.error = nil
        defer { followingState.isLoadingNextPage = false }

        let cursors = followingState.cursors
        guard !cursors.isEmpty else {
            followingState.hasMorePages = false
            return
        }

        followingState.cursors = [:]

        // Look up cached profiles for existing DIDs.
        let existingProfiles: [String: BSkyActorProfile] = {
            var map: [String: BSkyActorProfile] = [:]
            for item in followingState.items {
                if let profile = item.authorProfile {
                    map[item.document.authorDID] = profile
                }
            }
            return map
        }()
        let existingPublications: [String: PublicationEntry] = {
            var map: [String: PublicationEntry] = [:]
            for item in followingState.items {
                if let publication = item.publication {
                    map[publication.uri] = publication
                }
            }
            return map
        }()

        await withTaskGroup(of: (did: String, items: [ReaderFeedItem], cursor: String?).self) { group in
            for (did, cursor) in cursors {
                group.addTask { [did, cursor] in
                    let (records, nextCursor) = (try? await loginStateManager.listRecordsPage(
                        from: did,
                        collection: SiteStandardLexicon.DocumentRecord.type,
                        limit: 25,
                        cursor: cursor
                    )) ?? ([], nil)

                    let profile: BSkyActorProfile?
                    if let existing = existingProfiles[did] {
                        profile = existing
                    } else {
                        profile = try? await BSkyProfileFetcher.fetchProfile(did: did)
                    }
                    let items: [ReaderFeedItem] = records.compactMap { record in
                        guard let value = record.value,
                              let doc = value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
                            return nil
                        }
                        return ReaderFeedItem(
                            document: DocumentEntry(uri: record.uri, cid: record.cid, authorDID: did, record: doc),
                            publication: existingPublications[doc.site],
                            authorProfile: profile
                        )
                    }
                    return (did, items, nextCursor)
                }
            }
            for await result in group {
                followingState.items.append(contentsOf: result.items)
                if let cursor = result.cursor {
                    followingState.cursors[result.did] = cursor
                }
            }
        }

        followingState.items = deduplicated(followingState.items)
        followingState.hasMorePages = !followingState.cursors.isEmpty

        // Update cache with new items.
        let cachedItems = followingState.items.map { $0.toCachedFeedItem() }
        try? await feedCache.save(items: Array(cachedItems))
    }

    // MARK: Yours — eager (own documents are typically few)

    private func loadYoursFeed(loginStateManager: LoginStateManager) async {
        isLoadingYours = true
        yoursError = nil
        defer { isLoadingYours = false }

        do {
            async let ownPublications = loginStateManager.fetchPublicationsWithURIs()
            async let ownDocuments = loginStateManager.fetchDocumentsWithURIs()

            let (publications, documents) = try await (ownPublications, ownDocuments)

            // Resolve the user's own profile.
            let ownProfile: BSkyActorProfile?
            if let did = documents.first?.authorDID {
                ownProfile = try? await BSkyProfileFetcher.fetchProfile(did: did)
            } else {
                ownProfile = nil
            }

            yours = documents.map { document in
                ReaderFeedItem(
                    document: document,
                    publication: publications.first(where: { pub in
                        sharedDocumentBelongsToPublication(
                            documentSite: document.record.site,
                            publicationUri: pub.uri,
                            publicationUrl: pub.record.url
                        )
                    }),
                    authorProfile: ownProfile
                )
            }
            yours.sort(by: ReaderFeedItem.comparator(for: ReaderSortSettings.shared.sortOrder))
        } catch {
            yoursError = error.localizedDescription
        }
    }

    // MARK: - Helpers

    private func deduplicated(_ items: [ReaderFeedItem]) -> [ReaderFeedItem] {
        var entries: [String: ReaderFeedItem] = [:]
        for item in items {
            guard let existing = entries[item.id] else {
                entries[item.id] = item
                continue
            }

            // Cached items deliberately render before the publication lookup
            // completes. Prefer the later, enriched item once it arrives so
            // the feed card inherits its linked publication's theme.
            if (existing.isCached && !item.isCached) ||
                (existing.publication == nil && item.publication != nil) {
                entries[item.id] = item
            }
        }
        return entries.values
            .sorted(by: ReaderFeedItem.comparator(for: ReaderSortSettings.shared.sortOrder))
    }
}

struct ReaderFeedItem: Identifiable {
    let document: DocumentEntry
    let publication: PublicationEntry?
    let authorProfile: BSkyActorProfile?
    let isCached: Bool

    nonisolated init(
        document: DocumentEntry,
        publication: PublicationEntry?,
        authorProfile: BSkyActorProfile?,
        isCached: Bool = false
    ) {
        self.document = document
        self.publication = publication
        self.authorProfile = authorProfile
        self.isCached = isCached
    }

    var id: String { document.uri }

    @MainActor
    var moderationPresentation: ContentModerationPresentation {
        contentModerationPresentation(
            title: document.record.title,
            description: document.record.description,
            textContent: document.record.textContent,
            labels: (document.record.labels?.values.map(\.value) ?? []) +
                (publication?.record.labels?.values.map(\.value) ?? [])
        )
    }

    nonisolated static func newestFirst(_ lhs: Self, _ rhs: Self) -> Bool {
        lhs.document.record.publishedAt > rhs.document.record.publishedAt
    }

    nonisolated static func oldestFirst(_ lhs: Self, _ rhs: Self) -> Bool {
        lhs.document.record.publishedAt < rhs.document.record.publishedAt
    }

    nonisolated static func comparator(for sortOrder: ReaderSortOrder) -> (Self, Self) -> Bool {
        switch sortOrder {
        case .newestFirst: newestFirst
        case .oldestFirst: oldestFirst
        }
    }
}
