//
//  StandardReaderAPI.swift
//  Inkwell
//
//  Search client for pub search (leaflet-search-backend.fly.dev), the
//  maintained cross-platform index recommended by Standard.site. The index
//  aggregates records across the AT Protocol firehose and returns discovery
//  metadata; Inkwell always fetches the authoritative record from the
//  author's own PDS before rendering or subscribing to anything.
//
//  Two search entry points exist: `search(query:)` for the Discover tab's
//  keyword search, and `search(for:)` which accepts a broader term and
//  shares the same backend — internally both hit the same `/search`
//  endpoint with the same `keyword` mode.
//

import Foundation

struct ReaderSearchResult: Identifiable, Codable, Equatable, Hashable {
    let type: String
    let uri: String
    let did: String
    let title: String
    let snippet: String?
    let createdAt: String?
    let rkey: String?
    let basePath: String?
    let platform: String?
    let path: String?
    let coverImage: String?
    let handle: String?

    var id: String { uri }
    var isPublication: Bool { type == "publication" }
    var isStandardSiteDocument: Bool {
        ATURI.parse(uri)?.collection == SiteStandardLexicon.DocumentRecord.type
    }

    var createdDate: Date? {
        guard let createdAt else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: createdAt) ?? ISO8601DateFormatter().date(from: createdAt)
    }

    var webURL: URL? {
        guard let basePath, !basePath.isEmpty else { return nil }
        let origin = basePath.hasPrefix("http") ? basePath : "https://\(basePath)"
        if isPublication { return URL(string: origin) }
        if let path, !path.isEmpty {
            return URL(string: path.hasPrefix("/") ? origin + path : origin + "/" + path)
        }
        guard platform == "leaflet", let rkey else { return nil }
        return URL(string: origin + "/" + rkey)
    }
}

struct ReaderSearchResponse: Decodable {
    let results: [ReaderSearchResult]
    let total: Int
    let hasMore: Bool
}

final class StandardReaderAPI {
    static let shared = StandardReaderAPI()

    private let baseURL = URL(string: "https://leaflet-search-backend.fly.dev")!
    private let session: URLSession

    init() {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.timeoutIntervalForRequest = 20
        configuration.timeoutIntervalForResource = 30
        session = URLSession(configuration: configuration)
    }

    func search(query: String, limit: Int = 40) async throws -> ReaderSearchResponse {
        try await request("search", queryItems: [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "mode", value: "keyword"),
            URLQueryItem(name: "limit", value: String(max(1, min(limit, 100)))),
            URLQueryItem(name: "format", value: "v2")
        ])
    }

    /// Search for documents by a term (publication name, URL, or topic).
    /// The search index aggregates across the AT Protocol firehose — this
    /// finds documents from ALL authors, not just one PDS.
    func search(for term: String, limit: Int = 50) async throws -> ReaderSearchResponse {
        let query = term.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else {
            return ReaderSearchResponse(results: [], total: 0, hasMore: false)
        }
        return try await request("search", queryItems: [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "mode", value: "keyword"),
            URLQueryItem(name: "limit", value: String(max(1, min(limit, 100)))),
            URLQueryItem(name: "format", value: "v2")
        ])
    }

    private func request(_ path: String, queryItems: [URLQueryItem]) async throws -> ReaderSearchResponse {
        var components = URLComponents(
            url: baseURL.appending(path: path),
            resolvingAgainstBaseURL: false
        )!
        components.queryItems = queryItems

        let (data, response) = try await session.data(from: components.url!)
        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(ReaderSearchResponse.self, from: data)
    }
}
