//
//  InkwellIntents.swift
//  Inkwell
//
//  App Intents for Siri, Shortcuts, and Apple Intelligence integration.
//  Exposes reader and writer actions so users can open Inkwell directly
//  into a specific tab or refresh their feed via voice or automation.
//

import AppIntents
import SwiftUI

// MARK: - Open Reader Intent

/// Opens the Inkwell reader to browse posts from subscriptions.
struct OpenReaderIntent: AppIntent {
    static let title: LocalizedStringResource = "Open Reader"
    static let description = IntentDescription(
        "Opens the Inkwell reader to browse posts from your subscribed publications.",
        categoryName: "Reader"
    )
    static let openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        // Post a notification that the host app observes to switch tabs.
        NotificationCenter.default.post(
            name: .inkwellOpenTab,
            object: nil,
            userInfo: [InkwellTabKey.tab: InkwellTab.reader.rawValue]
        )
        return .result()
    }
}

// MARK: - Open Writer Intent

/// Opens the Inkwell writer to compose a new post.
struct OpenWriterIntent: AppIntent {
    static let title: LocalizedStringResource = "Open Writer"
    static let description = IntentDescription(
        "Opens the Inkwell writer to compose a new standard.site post.",
        categoryName: "Writer"
    )
    static let openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        NotificationCenter.default.post(
            name: .inkwellOpenTab,
            object: nil,
            userInfo: [InkwellTabKey.tab: InkwellTab.writer.rawValue]
        )
        return .result()
    }
}

// MARK: - Open Discover Intent

/// Opens the Inkwell Discover tab to find new publications.
struct OpenDiscoverIntent: AppIntent {
    static let title: LocalizedStringResource = "Discover Publications"
    static let description = IntentDescription(
        "Opens Inkwell's Discover tab to find new publications and blogs.",
        categoryName: "Discover"
    )
    static let openAppWhenRun: Bool = true

    @MainActor
    func perform() async throws -> some IntentResult {
        NotificationCenter.default.post(
            name: .inkwellOpenTab,
            object: nil,
            userInfo: [InkwellTabKey.tab: InkwellTab.discover.rawValue]
        )
        return .result()
    }
}

// MARK: - Supporting Types

enum InkwellTab: String {
    case reader
    case discover
    case writer
}

enum InkwellTabKey {
    static let tab = "inkwellTab"
}

extension Notification.Name {
    /// Posted when an App Intent requests a tab switch. The host app's
    /// ContentView observes this to programmatically change the selected tab.
    static let inkwellOpenTab = Notification.Name("inkwellOpenTab")
}

// MARK: - App Shortcuts Provider

/// Registers the available Siri Shortcuts and App Intents for Inkwell.
struct InkwellAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: OpenReaderIntent(),
            phrases: [
                "Open reader in \(.applicationName)",
                "Open my reader in \(.applicationName)",
                "Show my feed in \(.applicationName)",
                "Read my posts in \(.applicationName)",
            ],
            shortTitle: "Open Reader",
            systemImageName: "book"
        )

        AppShortcut(
            intent: OpenWriterIntent(),
            phrases: [
                "Open writer in \(.applicationName)",
                "Write a post in \(.applicationName)",
                "Compose in \(.applicationName)",
            ],
            shortTitle: "Open Writer",
            systemImageName: "square.and.pencil"
        )

        AppShortcut(
            intent: OpenDiscoverIntent(),
            phrases: [
                "Discover publications in \(.applicationName)",
                "Find new blogs in \(.applicationName)",
            ],
            shortTitle: "Discover",
            systemImageName: "safari"
        )
    }
}
