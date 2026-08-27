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

private struct FeedReportTarget: Identifiable {
    let subject: String
    let recordCID: String?

    var id: String { subject }
}

private struct ReaderProfileRoute: Hashable {
    let did: String
}

struct BrowseDocumentsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(ConnectivityMonitor.self) private var connectivityMonitor
    @State private var notificationManager = NotificationManager.shared
    @State private var store = ReaderFeedStore.shared

    @State private var showAbout = false
    @State private var showNotifications = false
    @State private var path = NavigationPath()
    @State private var reportTarget: FeedReportTarget?
    @State private var reportMessage: String?

    var body: some View {
        NavigationStack(path: $path) {
            content
                .safeAreaInset(edge: .top, spacing: 0) {
                    if !connectivityMonitor.isOnline {
                        OfflineStatusBanner()
                    }
                }
                // As a safe-area bar rather than a plain VStack row (on iOS
                // 26+), the feed switcher picks up the scroll-edge effect:
                // content slides under it and it stays legible over
                // whatever's passing behind, instead of sitting on an
                // opaque band. Older OS versions fall back to a plain
                // safe-area inset without that effect.
                .modifier(FeedSwitcherBar(selection: $store.selectedFeed))
                .onReceive(NotificationCenter.default.publisher(for: .moderationSettingsChanged)) { _ in
                    store.refreshModeration()
                }
                .navigationTitle("Reader")
                .navigationDestination(for: String.self) { documentURI in
                    RemoteDocumentView(documentURI: documentURI)
                }
                .navigationDestination(for: ReaderProfileRoute.self) { route in
                    ProfileView(did: route.did)
                }
                .toolbar {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Notifications", systemImage: "bell") {
                            showNotifications = true
                        }
                    }
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("Refresh", systemImage: "arrow.clockwise") {
                            Task { await store.loadData(loginStateManager: loginStateManager, force: true) }
                        }
                        .disabled(store.followingState.isLoading || store.isLoadingYours)
                    }
                }
                .sheet(isPresented: $showNotifications) {
                    NotificationsView(onOpenDocument: { uri in path.append(uri) })
                }
                .sheet(item: $reportTarget) { target in
                    ReportSheet(
                        subject: target.subject,
                        recordCID: target.recordCID,
                        onSubmit: {
                            reportMessage = "Report submitted."
                        },
                        onError: { message in
                            reportMessage = "Report failed: \(message)"
                        }
                    )
                }
                .alert("Report", isPresented: Binding(
                    get: { reportMessage != nil },
                    set: { if !$0 { reportMessage = nil } }
                )) {
                    Button("OK", role: .cancel) {}
                } message: {
                    Text(reportMessage ?? "")
                }
                .accountToolbar(showAbout: $showAbout)
                .task {
                    // Usually already loading (or loaded) by now — kicked
                    // off proactively from ContentView as soon as the user
                    // authenticated, not lazily on this view's first
                    // appearance. This is a no-op in that case.
                    await store.loadData(loginStateManager: loginStateManager)
                    notificationManager.markAllAsRead()
                }
                // Posted by NotificationDelegate when a tapped local
                // notification names a document — pushes it onto this
                // tab's own stack rather than replacing whatever's showing.
                .onReceive(NotificationCenter.default.publisher(for: .inkwellOpenDocument)) { notification in
                    guard let uri = notification.userInfo?[InkwellDocumentKey.uri] as? String else { return }
                    path.append(uri)
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
                        switch store.moderationPresentation(for: item) {
                        case .visible:
                            ZStack(alignment: .topTrailing) {
                                NavigationLink {
                                    ReadView(
                                        document: item.document.record,
                                        publication: item.publication?.record,
                                        documentURI: item.document.uri,
                                        documentCID: item.document.cid,
                                        authorDID: item.document.authorDID,
                                        previousItem: index > 0 ? store.followingState.items[index - 1] : nil,
                                        nextItem: index < store.followingState.items.count - 1 ? store.followingState.items[index + 1] : nil
                                    )
                                } label: {
                                    ReaderPostCard(item: item, reservesOverflowSpace: true)
                                }
                                .buttonStyle(.readerCard)

                                reportMenu(for: item)
                                    .padding(10)
                            }
                        case .warning, .hidden:
                            ModeratedReaderPostCard(
                                presentation: store.moderationPresentation(for: item),
                                onReveal: { store.revealModeratedItem(item) }
                            )
                        }
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
                        switch store.moderationPresentation(for: item) {
                        case .visible:
                            ZStack(alignment: .topTrailing) {
                                NavigationLink {
                                    ReadView(
                                        document: item.document.record,
                                        publication: item.publication?.record,
                                        documentURI: item.document.uri,
                                        documentCID: item.document.cid,
                                        authorDID: item.document.authorDID,
                                        previousItem: index > 0 ? store.yours[index - 1] : nil,
                                        nextItem: index < store.yours.count - 1 ? store.yours[index + 1] : nil
                                    )
                                } label: {
                                    ReaderPostCard(item: item, reservesOverflowSpace: true)
                                }
                                .buttonStyle(.readerCard)

                                reportMenu(for: item)
                                    .padding(10)
                            }
                        case .warning, .hidden:
                            ModeratedReaderPostCard(
                                presentation: store.moderationPresentation(for: item),
                                onReveal: { store.revealModeratedItem(item) }
                            )
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .refreshable { await store.loadData(loginStateManager: loginStateManager, force: true) }
        }
    }

    private func reportMenu(for item: ReaderFeedItem) -> some View {
        Menu {
            if !item.document.authorDID.isEmpty {
                Button {
                    path.append(ReaderProfileRoute(did: item.document.authorDID))
                } label: {
                    Label("View Profile", systemImage: "person.circle")
                }

                Divider()
            }
            Button {
                reportTarget = FeedReportTarget(
                    subject: item.document.uri,
                    recordCID: item.document.cid
                )
            } label: {
                Label("Report Post", systemImage: "doc.text")
            }
            if !item.document.authorDID.isEmpty {
                Button {
                    reportTarget = FeedReportTarget(subject: item.document.authorDID, recordCID: nil)
                } label: {
                    Label("Report Account", systemImage: "person")
                }
            }
        } label: {
            Image(systemName: "ellipsis")
                .font(.body.weight(.bold))
                .frame(width: 44, height: 44)
                .background(.ultraThinMaterial, in: Circle())
        }
        .accessibilityLabel("More actions for \(item.document.record.title)")
    }
}

private struct FeedSwitcherBar: ViewModifier {
    @Binding var selection: ReaderFeed

    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.safeAreaBar(edge: .top) { picker }
        } else {
            content.safeAreaInset(edge: .top) { picker }
        }
    }

    private var picker: some View {
        Picker("Reader feed", selection: $selection) {
            ForEach(ReaderFeed.allCases) { feed in
                Text(feed.rawValue).tag(feed)
            }
        }
        .pickerStyle(.segmented)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}
