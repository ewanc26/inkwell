//
//  ReaderFeedStore.swift
//  Inkwell
//
//  The reader feed's shared loaded state and fetch logic — see
//  BrowseDocumentsView.swift for the view that displays it.
//

import Foundation
import ATProtoKit

// MARK: - Pagination State

/// Tracks cursor-based pagination across multiple subscribed publications.
/// Each DID gets its own cursor; items are merged into a single feed sorted by
/// `publishedAt` descending.
@MainActor
struct FollowingFeedState {
    var items: [ReaderFeedItem] = []
    var isLoading = true        // true during initial load
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
    var isLoadingYours = true
    var yoursError: String?

    // MARK: - Data Loading

    /// Fetches both feeds. Skipped if a load is already in flight or has
    /// already completed once, unless `force` is set (pull-to-refresh, the
    /// manual reload button) — callers that just want loading to have
    /// *started* (e.g. as soon as the user's authenticated) don't need to
    /// re-trigger it themselves once it already has.
    func loadData(loginStateManager: LoginStateManager, force: Bool = false) async {
        guard force || !(followingState.hasLoaded || followingState.isLoading) else { return }
        async let _following: () = loadFollowingFeed(loginStateManager: loginStateManager)
        async let _yours: () = loadYoursFeed(loginStateManager: loginStateManager)
        _ = await (_following, _yours)
    }

    // MARK: Following — paginated

    /// Fetches subscriptions and the first page of documents from each followed
    /// publication. Subsequent pages are loaded on-demand via the sentinel.
    private func loadFollowingFeed(loginStateManager: LoginStateManager) async {
        followingState.isLoading = true
        followingState.error = nil
        defer {
            followingState.isLoading = false
            followingState.hasLoaded = true
        }

        do {
            let subscriptions = try await loginStateManager.fetchSubscriptions()

            // Reset state
            followingState.items = []
            followingState.cursors = [:]
            followingState.hasMorePages = !subscriptions.isEmpty

            guard !subscriptions.isEmpty else { return }

            // Collect unique DIDs for profile resolution.
            let uniqueDIDs = Set(subscriptions.compactMap { sub in
                parseAtUri(sub.record.publication)?.did
            })
            let profiles = await resolveProfiles(dids: uniqueDIDs)

            // Fetch first page from each subscribed publication concurrently.
            await withTaskGroup(of: (did: String, items: [ReaderFeedItem], cursor: String?).self) { group in
                for subscription in subscriptions {
                    let pubURI = subscription.record.publication
                    guard let pubDID = parseAtUri(pubURI)?.did else { continue }
                    group.addTask { [pubURI, pubDID] in
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
                                document: DocumentEntry(uri: record.uri, authorDID: pubDID, record: doc),
                                publication: pubEntry,
                                authorProfile: profile
                            )
                        }
                        return (pubDID, items, cursor)
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

            // Check if any DID still has more pages.
            followingState.hasMorePages = !followingState.cursors.isEmpty
        } catch {
            followingState.error = error.localizedDescription
        }
    }

    /// Resolves Bluesky profiles for a set of DIDs concurrently.
    private func resolveProfiles(dids: Set<String>) async -> [String: BSkyActorProfile] {
        await withTaskGroup(of: (String, BSkyActorProfile?).self) { group in
            for did in dids {
                group.addTask {
                    let profile = try? await BSkyProfileFetcher.fetchProfile(did: did)
                    return (did, profile)
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
                            document: DocumentEntry(uri: record.uri, authorDID: did, record: doc),
                            publication: nil,
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
            yours.sort(by: ReaderFeedItem.newestFirst)
        } catch {
            yoursError = error.localizedDescription
        }
    }

    // MARK: - Helpers

    private func deduplicated(_ items: [ReaderFeedItem]) -> [ReaderFeedItem] {
        var seen = Set<String>()
        return items
            .sorted(by: ReaderFeedItem.newestFirst)
            .filter { seen.insert($0.id).inserted }
    }
}

struct ReaderFeedItem: Identifiable {
    let document: DocumentEntry
    let publication: PublicationEntry?
    let authorProfile: BSkyActorProfile?

    var id: String { document.uri }

    nonisolated static func newestFirst(_ lhs: Self, _ rhs: Self) -> Bool {
        lhs.document.record.publishedAt > rhs.document.record.publishedAt
    }
}
