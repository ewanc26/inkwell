//
//  BSkyListFetcher.swift
//  Inkwell
//
//  Fetches Bluesky list members from the public Bluesky API
//  (public.api.bsky.app) so no authentication is needed. Powers the
//  "Supporters" section in CreditsView — people who've tipped via Ko-fi
//  or GitHub Sponsors, curated manually by ewancroft.uk as a Bluesky list.
//

import Foundation
import OSLog

// MARK: - Known Lists

enum SupportersList {
    /// Inkwell's Bluesky supporters list, owned by ewancroft.uk. Mirrors
    /// `SupportersList.URI` in the shared KMP module — kept as a duplicated
    /// literal rather than bridged through the XCFramework, matching
    /// `UserInputFeedback.inkwellSpaceURI`'s convention for one-off,
    /// developer-owned AT-URIs.
    static let uri = "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27"
}

// MARK: - API Response

// Internal rather than private so `BSkyListModelsTests` can decode a
// captured getList payload directly, mirroring Android's
// `BlueskyListModelsTest`.
struct BSkyListItem: Decodable, Sendable {
    let subject: BSkyActorProfile
}

struct GetListResponse: Decodable, Sendable {
    let items: [BSkyListItem]
    let cursor: String?
}

// MARK: - Fetcher

/// Fetches Bluesky list members from the public API (no auth required).
enum BSkyListFetcher {
    private static let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "BSkyListFetcher")
    private static let baseURL = sharedPublicBskyApi()
    private static let pageLimit = 100
    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 10
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config)
    }()

    /// Fetches every member of the list at `listUri`, following pagination
    /// cursors until exhausted. Returns an empty array on any failure
    /// rather than throwing — this powers a credits section, not a
    /// critical path.
    static func fetchListMembers(listUri: String) async -> [BSkyActorProfile] {
        var members: [BSkyActorProfile] = []
        var cursor: String?

        repeat {
            var components = URLComponents(string: "\(baseURL)\(sharedXrpcGraphGetList())")
            var queryItems = [
                URLQueryItem(name: "list", value: listUri),
                URLQueryItem(name: "limit", value: String(pageLimit)),
            ]
            if let cursor {
                queryItems.append(URLQueryItem(name: "cursor", value: cursor))
            }
            components?.queryItems = queryItems

            guard let url = components?.url else {
                logger.error("[BSkyListFetcher] Failed to build URL for list \(listUri)")
                return members
            }

            do {
                let (data, response) = try await session.data(from: url)
                guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
                    logger.error("[BSkyListFetcher] Bad response fetching list \(listUri)")
                    return members
                }
                let page = try JSONDecoder().decode(GetListResponse.self, from: data)
                members.append(contentsOf: page.items.map(\.subject))
                cursor = page.cursor
            } catch {
                logger.error("[BSkyListFetcher] Error fetching list \(listUri): \(error.localizedDescription)")
                return members
            }
        } while cursor != nil

        return members
    }
}
