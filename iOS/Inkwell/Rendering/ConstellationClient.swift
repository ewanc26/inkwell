//
//  ConstellationClient.swift
//  Inkwell
//
//  Queries the microcosm.blue Constellation API — a global AT Protocol
//  backlink index — to discover records (comments, quotes, mentions)
//  from ANY repository that reference a given subject URI.
//
//  Public instance: https://constellation.microcosm.blue
//

import Foundation
import OSLog

// MARK: - ConstellationClient

/// Queries the Constellation backlink index for cross-repo record discovery.
///
/// Without Constellation (or a similar backend), AT Protocol records stored
/// in other users' repositories are undiscoverable — each user's records are
/// siloed in their own PDS. Constellation indexes the firehose so you can ask
/// "which records across the entire network link to URI X?"
///
/// This client only does *discovery* — it returns backlink metadata (DID,
/// collection, rkey). To get the actual record content, hydrate each backlink
/// via `com.atproto.repo.getRecord` on the commenter's PDS.
enum ConstellationClient {
    private static let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Constellation")
    private static let baseURL = "https://constellation.microcosm.blue"

    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        config.httpAdditionalHeaders = [
            "Accept": "application/json",
            "User-Agent": "Inkwell/1.0",
        ]
        return URLSession(configuration: config)
    }()

    // MARK: - Backlink Types

    /// A single backlink from Constellation's `getBacklinks` response.
    struct Backlink: Decodable, Sendable {
        let did: String
        let collection: String
        let rkey: String

        /// The full AT-URI of the linking record, constructed from its parts.
        var recordURI: String {
            "at://\(did)/\(collection)/\(rkey)"
        }
    }

    /// Response wrapper for `getBacklinks`.
    private struct GetBacklinksResponse: Decodable {
        let records: [Backlink]?
        let cursor: String?
    }

    // MARK: - Public API

    /// Finds all records (across the entire AT Protocol network) that link to
    /// the given subject via the given source collection + path.
    ///
    /// - Parameters:
    ///   - subject: The target AT-URI, DID, or HTTPS URL.
    ///   - source: The source collection and JSON path, joined by `:`.
    ///     e.g. `"pub.leaflet.comment:subject"` for comments on a document.
    ///   - limit: Maximum backlinks per page (≤ 100).
    ///   - cursor: Pagination cursor from a previous page.
    /// - Returns: Backlinks and an optional cursor for the next page.
    static func getBacklinks(
        subject: String,
        source: String,
        limit: Int = 50,
        cursor: String? = nil
    ) async throws -> (backlinks: [Backlink], cursor: String?) {
        guard var components = URLComponents(string: "\(baseURL)/xrpc/blue.microcosm.links.getBacklinks") else {
            throw URLError(.badURL)
        }

        var queryItems = [
            URLQueryItem(name: "subject", value: subject),
            URLQueryItem(name: "source", value: source),
            URLQueryItem(name: "limit", value: String(min(limit, 100))),
        ]
        if let cursor {
            queryItems.append(URLQueryItem(name: "cursor", value: cursor))
        }
        components.queryItems = queryItems

        guard let url = components.url else {
            throw URLError(.badURL)
        }

        logger.debug("[Constellation] getBacklinks subject=\(subject) source=\(source)")

        let (data, response) = try await session.data(from: url)

        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200...299).contains(http.statusCode) else {
            logger.error("[Constellation] HTTP \(http.statusCode) for \(url.absoluteString)")
            throw URLError(.badServerResponse)
        }

        let decoded = try JSONDecoder().decode(GetBacklinksResponse.self, from: data)
        let backlinks = decoded.records ?? []
        logger.info("[Constellation] getBacklinks returned \(backlinks.count) backlinks, cursor=\(decoded.cursor ?? "nil")")
        return (backlinks, decoded.cursor)
    }

    /// Finds all comment backlinks (`pub.leaflet.comment`) referencing a
    /// standard.site document AT-URI.
    ///
    /// Paginates through all results automatically.
    static func getCommentBacklinks(
        documentURI: String,
        maximumCount: Int = 200
    ) async -> [Backlink] {
        await paginateBacklinks(
            subject: documentURI,
            source: "pub.leaflet.comment:subject",
            maximumCount: maximumCount
        )
    }

    /// Finds all recommend backlinks (`site.standard.graph.recommend`)
    /// referencing a document AT-URI.
    static func getRecommendBacklinks(
        documentURI: String,
        maximumCount: Int = 200
    ) async -> [Backlink] {
        await paginateBacklinks(
            subject: documentURI,
            source: "site.standard.graph.recommend:document",
            maximumCount: maximumCount
        )
    }

    /// Finds Bluesky posts that link to a URL via facets or external embeds.
    ///
    /// This mirrors leaflet.pub's `getConstellationBacklinks()` which queries
    /// two source paths to catch both link-in-text and link-card embeds.
    static func getDocumentMentionBacklinks(
        url: String,
        maximumCount: Int = 200
    ) async -> [Backlink] {
        let sourceFacets = "app.bsky.feed.post:facets[].features[app.bsky.richtext.facet#link].uri"
        let sourceEmbeds = "app.bsky.feed.post:embed.external.uri"

        async let facetLinks = paginateBacklinks(
            subject: url,
            source: sourceFacets,
            maximumCount: maximumCount
        )
        async let embedLinks = paginateBacklinks(
            subject: url,
            source: sourceEmbeds,
            maximumCount: maximumCount
        )

        let (facets, embeds) = await (facetLinks, embedLinks)
        var seen = Set<String>()
        var merged: [Backlink] = []
        for link in facets + embeds {
            let key = "\(link.did):\(link.rkey)"
            if seen.insert(key).inserted {
                merged.append(link)
            }
        }
        return merged
    }

    // MARK: - Private

    /// Paginates through `getBacklinks` results until exhausted or the limit
    /// is reached.
    private static func paginateBacklinks(
        subject: String,
        source: String,
        maximumCount: Int
    ) async -> [Backlink] {
        var all: [Backlink] = []
        var cursor: String?

        repeat {
            guard let result = try? await getBacklinks(
                subject: subject,
                source: source,
                limit: 50,
                cursor: cursor
            ) else { break }

            all.append(contentsOf: result.backlinks)
            cursor = result.cursor
        } while cursor != nil && all.count < maximumCount

        return Array(all.prefix(maximumCount))
    }
}
