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
//  Shared state/fetch logic lives in ReaderFeedStore.swift; the feed card
//  lives in ReaderPostCard.swift.
//

import SwiftUI
import ATProtoKit

struct BrowseDocumentsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var notificationManager = NotificationManager.shared
    @State private var store = ReaderFeedStore.shared

    @State private var showAbout = false

    var body: some View {
        NavigationStack {
            content
                // As a safe-area bar rather than a plain VStack row, the
                // feed switcher picks up the scroll-edge effect: content
                // slides under it and it stays legible over whatever's
                // passing behind, instead of sitting on an opaque band.
                .safeAreaBar(edge: .top) {
                    Picker("Reader feed", selection: $store.selectedFeed) {
                        ForEach(ReaderFeed.allCases) { feed in
                            Text(feed.rawValue).tag(feed)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }
                .navigationTitle("Reader")
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Refresh", systemImage: "arrow.clockwise") {
                            Task { await store.loadData(loginStateManager: loginStateManager, force: true) }
                        }
                        .disabled(store.followingState.isLoading || store.isLoadingYours)
                    }
                }
                .accountToolbar(showAbout: $showAbout)
                .task {
                    if CommandLine.arguments.contains("-screenshot") {
                        let mockDoc = SiteStandardLexicon.DocumentRecord(
                            site: "https://ewancroft.uk",
                            title: "Publishing on the Open Web with Standard.site",
                            publishedAt: Date(),
                            path: "/publishing-on-open-web",
                            description: "The Standard.site publishing spec brings structured, portable records to AT Protocol.",
                            // Without this, ReadView has nothing to render
                            // and falls back to its "Empty Document"
                            // placeholder — not what a reader-app screenshot
                            // should show when you open the one mock article.
                            textContent: """
                            Standard.site is a lightweight publishing lexicon for AT Protocol: your posts live in your own PDS as structured records, not locked inside any single app's database.

                            Any client that speaks the lexicon — Inkwell, a website, a future reader nobody's built yet — can read, verify, and render them the same way.

                            That's the whole pitch: write once, own the record, let it outlive the app.
                            """
                        )
                        let mockPub = SiteStandardLexicon.PublicationRecord(
                            url: "https://ewancroft.uk",
                            name: "Ewan's Corner",
                            description: "Essays on open protocols, software, and digital garden notes."
                        )
                        let mockItem = ReaderFeedItem(
                            document: DocumentEntry(uri: "at://did:plc:ewan/site.standard.document/1", authorDID: "did:plc:ewan", record: mockDoc),
                            publication: PublicationEntry(uri: "at://did:plc:ewan/site.standard.publication/1", authorDID: "did:plc:ewan", record: mockPub),
                            authorProfile: nil
                        )
                        store.followingState.items = [mockItem]
                        store.followingState.isLoading = false
                        store.followingState.hasLoaded = true
                        // Otherwise the "Yours" tab is stuck on its loading
                        // spinner forever in screenshot mode — nothing else
                        // ever flips isLoadingYours since loadYoursFeed()
                        // (a real network call) is skipped entirely here.
                        store.isLoadingYours = false
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
            ProgressView("Loading your reader…")
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = store.followingState.error, store.followingState.items.isEmpty {
            ContentUnavailableView {
                Label("Reader Unavailable", systemImage: "exclamationmark.triangle")
            } description: {
                Text(error)
            } actions: {
                Button("Try Again") {
                    Task { await store.loadData(loginStateManager: loginStateManager, force: true) }
                }
                .buttonStyle(.borderedProminent)
            }
        } else if store.followingState.items.isEmpty && store.followingState.hasLoaded {
            ContentUnavailableView {
                Label("Nothing to Read Yet", systemImage: "books.vertical")
            } description: {
                Text("Subscribe to publications in Discover and their latest posts will appear here.")
            } actions: {
                // An empty state that just describes the fix is a dead end;
                // this takes you to it. Routed through the same notification
                // the App Intents post, so tab switching stays in one place.
                Button("Browse Discover") {
                    NotificationCenter.default.post(
                        name: .inkwellOpenTab,
                        object: nil,
                        userInfo: [InkwellTabKey.tab: InkwellTab.discover.rawValue]
                    )
                }
                .buttonStyle(.borderedProminent)
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
                        .buttonStyle(.readerCard)
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
                Text("Loading more…")
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
            ProgressView("Loading your posts…")
                .controlSize(.large)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = store.yoursError, store.yours.isEmpty {
            ContentUnavailableView {
                Label("Posts Unavailable", systemImage: "exclamationmark.triangle")
            } description: {
                Text(error)
            } actions: {
                Button("Try Again") {
                    Task { await store.loadData(loginStateManager: loginStateManager, force: true) }
                }
                .buttonStyle(.borderedProminent)
            }
        } else if store.yours.isEmpty {
            ContentUnavailableView {
                Label("No Published Posts", systemImage: "doc.text")
            } description: {
                Text("Posts you publish from Inkwell or another standard.site app will appear here.")
            } actions: {
                Button("Start Writing") {
                    NotificationCenter.default.post(
                        name: .inkwellOpenTab,
                        object: nil,
                        userInfo: [InkwellTabKey.tab: InkwellTab.writer.rawValue]
                    )
                }
                .buttonStyle(.borderedProminent)
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
                        .buttonStyle(.readerCard)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .refreshable { await store.loadData(loginStateManager: loginStateManager, force: true) }
        }
    }
}
