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

    var body: some View {
        if loginStateManager.isRestoringSession {
            restoringView
        } else if loginStateManager.isAuthenticated {
            authenticatedView
        } else {
            LoginView()
        }
    }

    /// Shown briefly on launch while `restoreSessionIfPossible()` checks the
    /// Keychain for an existing session. Mirrors `LoginView`'s header so
    /// there's no jarring swap once it resolves to either screen.
    private var restoringView: some View {
        VStack(spacing: 16) {
            Image("Inkwell")
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
            ProgressView()
        }
    }

    private var authenticatedView: some View {
        TabView {
            BrowseDocumentsView()
                .tabItem {
                    Label("Read", systemImage: "book")
                }
                .badge(notificationManager.unreadCount)

            DiscoverView()
                .tabItem {
                    Label("Discover", systemImage: "safari")
                }

            WriteView()
                .tabItem {
                    Label("Write", systemImage: "square.and.pencil")
                }
        }
        .task {
            // Request notification permission and poll for new documents
            // from subscribed publications on launch.
            await NotificationManager.shared.requestPermission()
            await NotificationManager.shared.pollForNewDocuments(loginStateManager: loginStateManager)
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            Task {
                await notificationManager.pollForNewDocuments(loginStateManager: loginStateManager)
            }
        }
    }

    /// A tab's content while its real feature hasn't been built yet — shares
    /// the same sign-out button and greeting header across every tab so that
    /// chrome stays consistent as tabs gain real content over time.
    private func placeholderTab(title: String, systemImage: String, description: String) -> some View {
        NavigationStack {
            ContentUnavailableView(
                title,
                systemImage: systemImage,
                description: Text(description)
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 8) {
                        avatarView
                        Text(timeZoneAwareGreeting)
                            .font(.headline)
                    }
                }
            }
        }
    }

    /// The account's Bluesky avatar, shown small next to the greeting. Falls
    /// back to a generic person icon while loading or if no avatar is set.
    /// Purely decorative — not a button, so it shouldn't read as tappable.
    private var avatarView: some View {
        AsyncImage(url: loginStateManager.avatarURL) { phase in
            if let image = phase.image {
                image
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 24, height: 24)
        .clipShape(Circle())
    }

    /// A greeting whose period of day (morning/afternoon/evening/night) is
    /// derived from the device's current local time, with the account's
    /// Bluesky display name appended when one is set. If no display name is
    /// available, the name is dropped entirely rather than falling back to
    /// the handle.
    private var timeZoneAwareGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let period: String
        switch hour {
        case 5..<12:
            period = "morning"
        case 12..<17:
            period = "afternoon"
        case 17..<22:
            period = "evening"
        default:
            period = "night"
        }

        if let name = loginStateManager.displayName {
            return "Good \(period), \(name)"
        } else {
            return "Good \(period)"
        }
    }
}

#Preview {
    ContentView()
        .environment(LoginStateManager())
}
