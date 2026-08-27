//
//  ContentView.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

struct ContentView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(ConnectivityMonitor.self) private var connectivityMonitor
    @Environment(\.scenePhase) private var scenePhase
    @State private var notificationManager = NotificationManager.shared
    @State private var tipPromptManager = TipPromptManager.shared

    /// Selection is the same `InkwellTab` the App Intents post, so a
    /// Shortcuts-driven tab switch is just an assignment — no mapping
    /// between intent cases and anonymous integer tags.
    @State private var selectedTab: InkwellTab = .reader
    @State private var previousTab: InkwellTab = .reader
    @State private var discoverPath = NavigationPath()
    @State private var showingTip = false
    @State private var testingNotice = TestingModeNotice.shared

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
            if let tab = TestingMode.initialTab {
                selectedTab = tab
            }
        }
        .task {
            if !TestingMode.suppressesInterruptions, tipPromptManager.shouldShowTip {
                showingTip = true
            }
        }
        // Start loading the Reader feed as soon as the user's
        // authenticated, not lazily once the Reader tab is first shown —
        // so it's often already loaded (or well underway) by the time they
        // actually tap over to it.
        .task(id: loginStateManager.isAuthenticated) {
            guard loginStateManager.isAuthenticated else {
                await OfflineMutationStore.shared.refresh(accountDID: nil)
                return
            }
            if connectivityMonitor.isOnline {
                _ = await OfflineMutationStore.shared.flush(loginStateManager: loginStateManager)
            }
            await ReaderFeedStore.shared.loadData(loginStateManager: loginStateManager)
        }
        .onChange(of: connectivityMonitor.isOnline) { _, isOnline in
            guard isOnline, loginStateManager.isAuthenticated else { return }
            Task {
                _ = await OfflineMutationStore.shared.flush(loginStateManager: loginStateManager)
            }
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
        .alert(
            "Testing mode",
            isPresented: Binding(
                get: { testingNotice.isPresented },
                set: { testingNotice.isPresented = $0 }
            )
        ) {
            Button("OK", role: .cancel) { testingNotice.blockedAction = nil }
        } message: {
            Text(
                testingNotice.blockedAction.map {
                    "You're in testing mode, so this action will not hit the network.\n\n\($0) was not sent."
                } ?? "You're in testing mode, so this action will not hit the network."
            )
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

            Tab("Discover", systemImage: "safari", value: InkwellTab.discover) {
                NavigationStack(path: $discoverPath) {
                    DiscoverView(path: $discoverPath)
                        .navigationDestination(for: String.self) { documentURI in
                            RemoteDocumentView(documentURI: documentURI)
                        }
                        .navigationDestination(for: PublicationResult.self) { publication in
                            PublicationDetailView(publication: publication, path: $discoverPath)
                        }
                }
            }

            Tab("Write", systemImage: "square.and.pencil", value: InkwellTab.writer) {
                WriteView()
            }
        }
        // Reading is the point of the app: give the feed and the article
        // back the tab bar's worth of screen as soon as you scroll into them.
        // tabBarMinimizeBehavior is iOS 26+; the app's floor is much lower,
        // so this only applies on devices new enough to have it.
        .modifier(MinimizeTabBarOnScrollDown())
        .onChange(of: selectedTab) { _, newTab in
            // When the user taps the already-selected tab, pop that tab's
            // navigation stack back to its root — matching the native
            // iOS tab-bar convention.
            if newTab == previousTab {
                switch newTab {
                case .discover: discoverPath = NavigationPath()
                default: break
                }
            }
            previousTab = newTab
        }
        .task {
            if !TestingMode.suppressesInterruptions {
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
        .environment(ConnectivityMonitor())
}

private struct MinimizeTabBarOnScrollDown: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.tabBarMinimizeBehavior(.onScrollDown)
        } else {
            content
        }
    }
}
