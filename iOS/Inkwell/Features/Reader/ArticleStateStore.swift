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

    /// Versioned portable envelope for the Settings → Export Data action.
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

        let envelope = ReadingDataExport(
            format: "uk.ewancroft.inkwell.reading-data",
            version: 1,
            exportedAt: Date(),
            articles: items
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return try? encoder.encode(envelope)
    }

    enum ImportError: LocalizedError {
        case invalidEnvelope
        case unsupportedVersion
        case invalidArticle

        var errorDescription: String? {
            switch self {
            case .invalidEnvelope: "This file is not a valid Inkwell reading-data export."
            case .unsupportedVersion: "This reading-data export uses an unsupported version."
            case .invalidArticle: "The export contains an invalid article entry."
            }
        }
    }

    /// Validates and merges an export without allowing older state to replace
    /// newer local choices. The mutation is applied only after every entry is
    /// validated, so malformed files cannot partially update the store.
    @discardableResult
    func importJSON(_ data: Data) throws -> Int {
        let envelope = try validatedImport(data)

        var merged = states
        var changes = 0
        for article in envelope.articles {
            if let existing = merged[article.articleId], existing.updatedAt >= article.timestamp { continue }
            merged[article.articleId] = ArticleState(
                title: article.title,
                isRead: article.isRead,
                isBookmarked: article.isBookmarked,
                updatedAt: article.timestamp
            )
            changes += 1
        }
        states = merged
        persist()
        return changes
    }

    /// Returns the number of local records that would change, without mutating state.
    func previewImportJSON(_ data: Data) throws -> Int {
        try validatedImport(data).articles.reduce(into: 0) { count, article in
            if states[article.articleId]?.updatedAt ?? .distantPast < article.timestamp { count += 1 }
        }
    }

    private func validatedImport(_ data: Data) throws -> ReadingDataExport {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let envelope = try decoder.decode(ReadingDataExport.self, from: data)
        guard envelope.format == "uk.ewancroft.inkwell.reading-data" else { throw ImportError.invalidEnvelope }
        guard envelope.version == 1 else { throw ImportError.unsupportedVersion }
        guard envelope.exportedAt <= Date() else { throw ImportError.invalidArticle }
        guard envelope.articles.count <= 10_000 else { throw ImportError.invalidArticle }
        for article in envelope.articles {
            guard parseAtUri(article.articleId) != nil, article.title.count <= 500, article.timestamp <= Date() else {
                throw ImportError.invalidArticle
            }
        }
        return envelope
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(states) else { return }
        defaults.set(data, forKey: storageKey)
    }
}

private struct ReadingDataExport: Codable {
    let format: String
    let version: Int
    let exportedAt: Date
    let articles: [ExportedArticleState]
}

private struct ExportedArticleState: Codable {
    let articleId: String
    let title: String
    let isRead: Bool
    let isBookmarked: Bool
    let timestamp: Date
}
