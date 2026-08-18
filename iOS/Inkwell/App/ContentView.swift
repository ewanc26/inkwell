//
//  ContentView.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

struct ContentView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.scenePhase) private var scenePhase
    @State private var notificationManager = NotificationManager.shared
    @State private var tipPromptManager = TipPromptManager.shared

    /// Selection is the same `InkwellTab` the App Intents post, so a
    /// Shortcuts-driven tab switch is just an assignment — no mapping
    /// between intent cases and anonymous integer tags.
    @State private var selectedTab: InkwellTab = .reader
    @State private var showingTip = false
    /// Bound to Discover's search field. Owned here rather than in
    /// DiscoverView because `.searchable` for a `Tab(role: .search)` must
    /// be attached to the TabView itself to actually appear — see the note
    /// on `DiscoverView.query`.
    @State private var discoverQuery = ""

    var body: some View {
        Group {
            if loginStateManager.isRestoringSession {
                restoringView
            } else if loginStateManager.isAuthenticated {
                authenticatedView
            } else {
                LoginView()
            }
        }
        .onAppear {
            if CommandLine.arguments.contains("-tab-discover") {
                selectedTab = .discover
            } else if CommandLine.arguments.contains("-tab-writer") {
                selectedTab = .writer
            } else if CommandLine.arguments.contains("-tab-reader") {
                selectedTab = .reader
            }
        }
        .task {
            if !CommandLine.arguments.contains("-screenshot") {
                if tipPromptManager.shouldShowTip {
                    showingTip = true
                }
            }
        }
        // Start loading the Reader feed as soon as the user's
        // authenticated, not lazily once the Reader tab is first shown —
        // so it's often already loaded (or well underway) by the time they
        // actually tap over to it.
        .task(id: loginStateManager.isAuthenticated) {
            guard loginStateManager.isAuthenticated, !CommandLine.arguments.contains("-screenshot") else { return }
            await ReaderFeedStore.shared.loadData(loginStateManager: loginStateManager)
        }
        .alert("Enjoying Inkwell?", isPresented: $showingTip) {
            Button("Maybe Later", role: .cancel) { tipPromptManager.markShown() }
            Button("Tip Me") {
                if let url = URL(string: "https://ko-fi.com/ewancroft") {
                    UIApplication.shared.open(url)
                }
                tipPromptManager.markShown()
            }
        } message: {
            Text("If you find Inkwell useful, consider buying me a coffee to support ongoing development.")
        }
        // Routes App Intent tab-switch notifications to the matching tab.
        // Posted by OpenReaderIntent, OpenWriterIntent, and
        // OpenDiscoverIntent when run via Siri or Shortcuts.
        .onReceive(NotificationCenter.default.publisher(for: .inkwellOpenTab)) { notification in
            guard let raw = notification.userInfo?[InkwellTabKey.tab] as? String,
                  let tab = InkwellTab(rawValue: raw) else { return }
            withAnimation {
                selectedTab = tab
            }
        }
    }

    /// Shown briefly on launch while `restoreSessionIfPossible()` checks the
    /// Keychain for an existing session. Mirrors `LoginView`'s header so
    /// there's no jarring swap once it resolves to either screen.
    private var restoringView: some View {
        VStack(spacing: 16) {
            InkwellMark()
                .frame(height: 48)
                .foregroundStyle(.primary)
            ProgressView()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(uiColor: .systemBackground))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Restoring your session")
    }

    private var authenticatedView: some View {
        TabView(selection: $selectedTab) {
            Tab("Read", systemImage: "book", value: InkwellTab.reader) {
                BrowseDocumentsView()
            }
            .badge(notificationManager.unreadCount)

            // Discover is search, and nothing else — the search role gives
            // it the system's dedicated search tab treatment rather than a
            // generic tab that happens to contain a search field.
            Tab("Discover", systemImage: "magnifyingglass", value: InkwellTab.discover, role: .search) {
                DiscoverView(query: $discoverQuery)
            }

            Tab("Write", systemImage: "square.and.pencil", value: InkwellTab.writer) {
                WriteView()
            }
        }
        // Reading is the point of the app: give the feed and the article
        // back the tab bar's worth of screen as soon as you scroll into them.
        .tabBarMinimizeBehavior(.onScrollDown)
        // Must live on the TabView, not inside DiscoverView — see the note
        // on DiscoverView.query.
        .searchable(text: $discoverQuery, prompt: "Publications and articles")
        .textInputAutocapitalization(.never)
        .autocorrectionDisabled()
        .tabViewSearchActivation(.searchTabSelection)
        .task {
            if !CommandLine.arguments.contains("-screenshot") {
                await NotificationManager.shared.requestPermission()
                await NotificationManager.shared.pollForNewDocuments(loginStateManager: loginStateManager)
            }
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            Task {
                await notificationManager.pollForNewDocuments(loginStateManager: loginStateManager)
            }
        }
    }
}

#Preview {
    ContentView()
        .environment(LoginStateManager())
}
