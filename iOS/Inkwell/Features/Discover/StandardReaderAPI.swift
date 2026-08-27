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
//  Keyword search is exposed through `search(query:)`, `search(query:mode:)`
//  (which lets the Discover tab request the backend's `publications` mode),
//  and `search(for:)` — all hitting the same `/search` endpoint.
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
    var isPublication: Bool { sharedIsPublication(type: type) }
    var isStandardSiteDocument: Bool {
        sharedIsStandardSiteDocument(uri: uri)
    }

    var createdDate: Date? {
        guard let createdAt else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter.date(from: createdAt) ?? ISO8601DateFormatter().date(from: createdAt)
    }

    var webURL: URL? {
        guard let basePath, !basePath.isEmpty else { return nil }
        guard let urlString = sharedWebURL(
            basePath: basePath,
            path: path,
            rkey: rkey,
            platform: platform,
            isPublication: isPublication
        ) else { return nil }
        return URL(string: urlString)
    }
}

struct ReaderSearchActorResult: Identifiable, Codable, Equatable, Hashable {
    let did: String
    let handle: String
    let displayName: String?
    let avatar: String?

    var id: String { did }
}

struct ReaderSearchActorResponse: Decodable {
    let actors: [ReaderSearchActorResult]
}

/// A distinct publication (site) derived from search results. The
/// leaflet-search-backend indexes documents, not publications, so a
/// publication is reconstructed by grouping results that share an author DID
/// and `basePath` (the publication's origin domain).
struct PublicationResult: Identifiable, Hashable {
    let name: String
    let domain: String
    let url: URL?
    let did: String
    let coverImage: String?

    var id: String { domain }

    /// Collapses document search results into distinct publications: results
    /// from the same author DID and `basePath` are one publication.
    static func aggregate(_ results: [ReaderSearchResult]) -> [PublicationResult] {
        let grouped = Dictionary(grouping: results) { "\($0.did)|\($0.basePath ?? "")" }
        return grouped.compactMap { (_, items) in
            guard let domain = items.first?.basePath, !domain.isEmpty else { return nil }
            let first = items.first!
            return PublicationResult(
                name: domain,
                domain: domain,
                url: URL(string: "https://\(domain)"),
                did: first.did,
                coverImage: items.compactMap { $0.coverImage }.first
            )
        }.sorted { $0.name < $1.name }
    }
}

struct ReaderSearchResponse: Decodable {
    let results: [ReaderSearchResult]
    let total: Int?
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

    /// Keyword search that allows an explicit backend `mode` (e.g.
    /// `"publications"`). The leaflet-search-backend indexes documents rather
    /// than publications, so the Publications scope aggregates the returned
    /// documents into distinct publications on the client.
    func search(query: String, mode: String, limit: Int = 40) async throws -> ReaderSearchResponse {
        try await request("search", queryItems: [
            URLQueryItem(name: "q", value: query),
            URLQueryItem(name: "mode", value: mode),
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

    func searchActors(query: String, limit: Int = 10) async throws -> ReaderSearchActorResponse {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return ReaderSearchActorResponse(actors: [])
        }
        let url = URL(string: "https://public.api.bsky.app/xrpc/app.bsky.actor.searchActorsTypeahead")!
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "q", value: trimmed),
            URLQueryItem(name: "limit", value: String(max(1, min(limit, 100))))
        ]

        let (data, response) = try await session.data(from: components.url!)
        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        return try JSONDecoder().decode(ReaderSearchActorResponse.self, from: data)
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
