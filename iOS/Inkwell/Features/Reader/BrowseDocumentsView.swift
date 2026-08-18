//
//  BrowseDocumentsView.swift
//  Inkwell
//
//  The main reader feed: a "Following" tab that merges documents from every
//  subscribed publication into one chronologically-sorted stream, and a
//  "Yours" tab showing the signed-in user's own published documents.
//  Pagination is cursor-based per-DID so each publication's feed page
//  advances independently.
//

import SwiftUI
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

struct BrowseDocumentsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var notificationManager = NotificationManager.shared
    @State private var store = ReaderFeedStore.shared

    @State private var showCredits = false

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Reader feed", selection: $store.selectedFeed) {
                    ForEach(ReaderFeed.allCases) { feed in
                        Text(feed.rawValue).tag(feed)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                content
            }
            .navigationTitle("Reader")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showCredits = true } label: {
                        Image(systemName: "info.circle")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        Task { await store.loadData(loginStateManager: loginStateManager, force: true) }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(store.followingState.isLoading || store.isLoadingYours)
                }
            }
            .sheet(isPresented: $showCredits) {
                CreditsView()
            }
            .task {
                if CommandLine.arguments.contains("-screenshot") {
                    let mockDoc = SiteStandardLexicon.DocumentRecord(
                        site: "https://ewancroft.uk",
                        title: "Publishing on the Open Web with Standard.site",
                        publishedAt: Date(),
                        path: "/publishing-on-open-web",
                        description: "The Standard.site publishing spec brings structured, portable records to AT Protocol."
                    )
                    let mockPub = SiteStandardLexicon.PublicationRecord(
                        url: "https://ewancroft.uk",
                        name: "Ewan's Corner",
                        description: "Essays on open protocols, software, and digital garden notes."
                    )
                    let mockItem = ReaderFeedItem(
                        document: DocumentEntry(uri: "at://did:plc:ewan/site.standard.document/1", authorDID: "did:plc:ewan", record: mockDoc),
                        publication: nil,
                        authorProfile: nil
                    )
                    store.followingState.items = [mockItem]
                    store.followingState.isLoading = false
                    store.followingState.hasLoaded = true
                } else {
                    // Usually already loading (or loaded) by now — kicked
                    // off proactively from ContentView as soon as the user
                    // authenticated, not lazily on this view's first
                    // appearance. This is a no-op in that case.
                    await store.loadData(loginStateManager: loginStateManager)
                    notificationManager.markAllAsRead()
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if store.selectedFeed == .following {
            followingContent
        } else {
            yoursContent
        }
    }

    // MARK: - Following feed

    @ViewBuilder
    private var followingContent: some View {
        if store.followingState.isLoading && store.followingState.items.isEmpty {
            ProgressView("Loading your reader...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = store.followingState.error, store.followingState.items.isEmpty {
            ContentUnavailableView(
                "Reader Unavailable",
                systemImage: "exclamationmark.triangle",
                description: Text(error)
            )
        } else if store.followingState.items.isEmpty && store.followingState.hasLoaded {
            ContentUnavailableView {
                Label("Nothing to read yet", systemImage: "books.vertical")
            } description: {
                Text("Subscribe to publications in Discover and their latest posts will appear here.")
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 18) {
                    ForEach(Array(store.followingState.items.enumerated()), id: \.element.id) { index, item in
                        NavigationLink {
                            ReadView(
                                document: item.document.record,
                                publication: item.publication?.record,
                                documentURI: item.document.uri,
                                authorDID: item.document.authorDID,
                                previousItem: index > 0 ? store.followingState.items[index - 1] : nil,
                                nextItem: index < store.followingState.items.count - 1 ? store.followingState.items[index + 1] : nil
                            )
                        } label: {
                            ReaderPostCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }

                    // Infinite-scroll sentinel
                    sentinel
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .refreshable { await store.loadData(loginStateManager: loginStateManager, force: true) }
        }
    }

    @ViewBuilder
    private var sentinel: some View {
        if store.followingState.isLoadingNextPage {
            HStack(spacing: 10) {
                ProgressView()
                Text("Loading more...")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 12)
        } else if let error = store.followingState.error {
            HStack(spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.orange)
                    .font(.caption)
                Text(error)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Button("Retry") {
                    Task { await store.loadNextFollowingPage(loginStateManager: loginStateManager) }
                }
                .font(.caption.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
        } else if store.followingState.hasMorePages && store.followingState.hasLoaded {
            Color.clear
                .frame(height: 1)
                .onAppear {
                    Task { await store.loadNextFollowingPage(loginStateManager: loginStateManager) }
                }
        } else if store.followingState.hasLoaded && !store.followingState.items.isEmpty {
            Text("You're all caught up")
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
        }
    }

    // MARK: - Yours feed

    @ViewBuilder
    private var yoursContent: some View {
        if store.isLoadingYours && store.yours.isEmpty {
            ProgressView("Loading your posts...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = store.yoursError, store.yours.isEmpty {
            ContentUnavailableView(
                "Reader Unavailable",
                systemImage: "exclamationmark.triangle",
                description: Text(error)
            )
        } else if store.yours.isEmpty {
            ContentUnavailableView {
                Label("No published posts", systemImage: "doc.text")
            } description: {
                Text("Posts you publish from Inkwell or another standard.site app will appear here.")
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 18) {
                    ForEach(Array(store.yours.enumerated()), id: \.element.id) { index, item in
                        NavigationLink {
                            ReadView(
                                document: item.document.record,
                                publication: item.publication?.record,
                                documentURI: item.document.uri,
                                authorDID: item.document.authorDID,
                                previousItem: index > 0 ? store.yours[index - 1] : nil,
                                nextItem: index < store.yours.count - 1 ? store.yours[index + 1] : nil
                            )
                        } label: {
                            ReaderPostCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .refreshable { await store.loadData(loginStateManager: loginStateManager, force: true) }
        }
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

private struct ReaderPostCard: View {
    let item: ReaderFeedItem
    @Environment(\.colorScheme) private var colorScheme

    private var document: SiteStandardLexicon.DocumentRecord { item.document.record }
    private var publication: SiteStandardLexicon.PublicationRecord? { item.publication?.record }

    // Same theme resolution as ReadView (Leaflet's rich theme falling back
    // to basicTheme, then system defaults) so a card in the feed matches
    // what the publication actually looks like once opened. Untethemed
    // documents fall back to a grouped-background card instead of
    // ReaderTheme's plain systemBackground default, so cards stay visually
    // distinct from the surrounding scroll view when no theme is set.
    private var theme: ReaderTheme {
        ReaderTheme(document: document, publication: publication, colorScheme: colorScheme)
    }

    private var hasExplicitTheme: Bool {
        document.theme != nil || publication?.theme != nil || publication?.basicTheme != nil
    }

    private var background: Color {
        hasExplicitTheme ? theme.background : Color(uiColor: .secondarySystemGroupedBackground)
    }
    private var foreground: Color { theme.foreground }
    private var accent: Color { theme.accent }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let coverURL {
                AsyncImage(url: coverURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .aspectRatio(16 / 9, contentMode: .fit)
                            .clipped()
                    case .failure, .empty:
                        EmptyView()
                    @unknown default:
                        EmptyView()
                    }
                }
            }

            VStack(alignment: .leading, spacing: 9) {
                HStack(spacing: 10) {
                    if let avatarURL = item.authorProfile?.avatar.flatMap({ URL(string: $0) }) {
                        AsyncImage(url: avatarURL) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                                    .frame(width: 28, height: 28)
                                    .clipShape(.circle)
                            case .failure, .empty:
                                EmptyView()
                            @unknown default:
                                EmptyView()
                            }
                        }
                    }
                    if let displayName = item.authorProfile?.displayName {
                        Text(displayName)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(foreground.opacity(0.7))
                    }
                }

                Text(document.title)
                    .font(theme.headingFont(.title3, weight: .bold))
                    .foregroundStyle(foreground)
                    .lineLimit(2)

                if let description = document.description, !description.isEmpty {
                    Text(description)
                        .font(theme.bodyFont(.subheadline))
                        .foregroundStyle(foreground.opacity(0.7))
                        .lineLimit(3)
                }

                HStack(spacing: 6) {
                    Text(formattedDate)
                    if let publicationName = publication?.name {
                        Text("·")
                        Text(publicationName)
                            .fontWeight(.semibold)
                            .foregroundStyle(accent)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                    Image(systemName: "arrow.up.right")
                }
                .font(.caption)
                .foregroundStyle(foreground.opacity(0.55))
            }
            .padding(16)
        }
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(foreground.opacity(0.1), lineWidth: 1)
        }
    }

    private var coverURL: URL? {
        guard let cover = document.coverImage else { return nil }
        return URL(string: "https://cdn.bsky.app/img/feed_thumbnail/plain/\(item.document.authorDID)/\(cover.reference.link)")
    }

    private var formattedDate: String {
        document.publishedAt.formatted(date: .abbreviated, time: .omitted)
    }
}
