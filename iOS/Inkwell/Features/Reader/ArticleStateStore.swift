//
//  ArticleStateStore.swift
//  Inkwell
//
//  Local (device-only) read/bookmark tracking, keyed by document AT-URI.
//  Mirrors AccessibilitySettings.swift / HapticsSettings.swift's
//  @Observable-singleton-backed-by-UserDefaults pattern — a full SwiftData
//  model felt like overkill for two booleans per article.
//

import SwiftUI
import Observation

struct ArticleState: Codable {
    var title: String
    var isRead: Bool = false
    var isBookmarked: Bool = false
    var updatedAt: Date = Date()
}

@MainActor
@Observable
final class ArticleStateStore {
    static let shared = ArticleStateStore()

    private let defaults = UserDefaults.standard
    private let storageKey = "reader.articleState"

    private(set) var states: [String: ArticleState] = [:]

    private init() {
        if let data = defaults.data(forKey: storageKey),
           let decoded = try? JSONDecoder().decode([String: ArticleState].self, from: data) {
            states = decoded
        }
    }

    func isRead(_ articleID: String) -> Bool { states[articleID]?.isRead ?? false }
    func isBookmarked(_ articleID: String) -> Bool { states[articleID]?.isBookmarked ?? false }

    /// Called when a document is opened. A no-op once already marked, so it
    /// doesn't keep bumping `updatedAt` on every re-visit.
    func markAsRead(_ articleID: String, title: String) {
        guard states[articleID]?.isRead != true else { return }
        var state = states[articleID] ?? ArticleState(title: title)
        state.title = title
        state.isRead = true
        state.updatedAt = Date()
        states[articleID] = state
        persist()
    }

    func setBookmarked(_ articleID: String, title: String, bookmarked: Bool) {
        var state = states[articleID] ?? ArticleState(title: title)
        state.title = title
        state.isBookmarked = bookmarked
        state.updatedAt = Date()
        states[articleID] = state
        persist()
    }

    /// Pretty-printed JSON array of `{articleId, title, isRead, isBookmarked,
    /// timestamp}` for the Settings → Export Data action.
    func exportJSON() -> Data? {
        let items = states.map { key, value in
            ExportedArticleState(
                articleId: key,
                title: value.title,
                isRead: value.isRead,
                isBookmarked: value.isBookmarked,
                timestamp: value.updatedAt
            )
        }
        .sorted { $0.timestamp > $1.timestamp }

        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return try? encoder.encode(items)
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(states) else { return }
        defaults.set(data, forKey: storageKey)
    }
}

private struct ExportedArticleState: Codable {
    let articleId: String
    let title: String
    let isRead: Bool
    let isBookmarked: Bool
    let timestamp: Date
}
