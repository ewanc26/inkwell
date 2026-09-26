//
//  BSkyPostModels.swift
//  Inkwell
//
//  Bluesky post models and the fetcher used by BSkyPostEmbed.swift.
//

import Foundation
import OSLog

// MARK: - Bluesky Post Model

/// A minimal Bluesky post view returned by `app.bsky.feed.getPosts`.
struct BSkyPostView: Decodable, Sendable, Identifiable {
    let uri: String
    let cid: String?
    let author: BSkyAuthor
    let record: BSkyPostRecord
    let replyCount: Int?
    let repostCount: Int?
    let likeCount: Int?
    let embed: BSkyEmbed?

    var id: String { uri }
}

struct BSkyAuthor: Decodable, Sendable {
    let did: String?
    let handle: String?
    let displayName: String?
    let avatar: String?
}

struct BSkyPostRecord: Decodable, Sendable {
    let text: String?
    let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case text
        case createdAt
    }
}

/// A union of possible Bluesky post embeds (image, external link, record).
enum BSkyEmbed: Decodable, Sendable {
    case images(BSkyImagesEmbed)
    case external(BSkyExternalEmbed)
    case record(BSkyRecordEmbed)
    case unknown

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let type = try container.decode(String.self, forKey: .type)

        switch type {
        case sharedBlueskyEmbedImages():
            self = .images(try BSkyImagesEmbed(from: decoder))
        case sharedBlueskyEmbedExternal():
            self = .external(try BSkyExternalEmbed(from: decoder))
        case sharedBlueskyEmbedRecord():
            self = .record(try BSkyRecordEmbed(from: decoder))
        default:
            self = .unknown
        }
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
    }
}

struct BSkyImagesEmbed: Decodable, Sendable {
    let images: [BSkyImage]

    struct BSkyImage: Decodable, Sendable {
        let thumb: String?
        let fullsize: String?
        let alt: String?
    }
}

struct BSkyExternalEmbed: Decodable, Sendable {
    let external: BSkyExternal

    /// A `app.bsky.embed.external#viewExternal` view. Bluesky's May 2026
    /// Standard.site integration enriches this shape with `readingTime`,
    /// `labels`, `source`, `associatedRefs`, and `associatedProfiles` on
    /// external links backed by a standard.site publication. Plain Bluesky
    /// external cards simply omit all of these, so every enriched field is
    /// optional and decoding degrades gracefully either way.
    struct BSkyExternal: Decodable, Sendable {
        let uri: String?
        let title: String?
        let description: String?
        let thumb: String?
        let createdAt: String?
        let updatedAt: String?
        let readingTime: Int?
        let labels: [BSkyLabel]?
        let source: BSkyExternalSource?
        let associatedRefs: [BSkyStrongRef]?
        let associatedProfiles: [BSkyAuthor]?
    }
}

struct BSkyStrongRef: Decodable, Sendable {
    let uri: String?
    let cid: String?
}

/// `app.bsky.embed.external#viewExternalSource` — identifies the
/// standard.site (or other) publication that backs an enriched external
/// embed.
struct BSkyExternalSource: Decodable, Sendable {
    let uri: String?
    let icon: String?
    let title: String?
    let description: String?
    let theme: BSkyExternalSourceTheme?
}

/// `app.bsky.embed.external#viewExternalSourceTheme`.
struct BSkyExternalSourceTheme: Decodable, Sendable {
    let backgroundRGB: BSkyColorRGB?
    let foregroundRGB: BSkyColorRGB?
    let accentRGB: BSkyColorRGB?
    let accentForegroundRGB: BSkyColorRGB?
}

/// `app.bsky.embed.external#colorRGB`.
struct BSkyColorRGB: Decodable, Sendable {
    let r: Int?
    let g: Int?
    let b: Int?
}

// MARK: - Display Helpers

/// Pure formatting/extraction logic for enriched external embeds, kept
/// separate from decoding so it can be unit tested without SwiftUI.
extension BSkyExternalEmbed.BSkyExternal {
    /// Whether this card carries Standard.site-enriched metadata (a
    /// source, reading time, or associated content) rather than being a
    /// plain Bluesky external-link card. Plain cards must keep rendering
    /// unchanged, so all enriched-only UI is gated on this.
    var isStandardSiteEnriched: Bool {
        source != nil || readingTime != nil || associatedRefs?.isEmpty == false
    }

    /// "5 min read", or `nil` when no positive reading time is available.
    var readingTimeLabel: String? {
        guard let readingTime, readingTime > 0 else { return nil }
        return "\(readingTime) min read"
    }

    /// Non-empty label values, suitable for a content-warning row. Negation
    /// labels and blank values are excluded since they don't represent an
    /// active warning to display.
    var contentWarningLabels: [String] {
        (labels ?? []).compactMap { label in
            guard label.neg != true, let val = label.val, !val.isEmpty else { return nil }
            return val
        }
    }

    /// The source's display title, falling back to its host when no title
    /// is present but a URI is.
    var sourceDisplayTitle: String? {
        if let title = source?.title, !title.isEmpty { return title }
        if let uri = source?.uri, let host = URL(string: uri)?.host { return host }
        return nil
    }
}

struct BSkyRecordEmbed: Decodable, Sendable {
    let record: BSkyEmbeddedRecord

    struct BSkyEmbeddedRecord: Decodable, Sendable {
        let uri: String?
        let cid: String?
        let author: BSkyAuthor?
        let value: BSkyEmbeddedRecordValue?

        struct BSkyEmbeddedRecordValue: Decodable, Sendable {
            let text: String?
            let createdAt: String?
        }
    }
}

// MARK: - API Response

private struct GetPostsResponse: Decodable, Sendable {
    let posts: [BSkyPostView]
}

// MARK: - Fetcher

/// Simple in-memory cache for fetched Bluesky posts.
private actor BSkyPostCache {
    static let shared = BSkyPostCache()
    private var storage: [String: BSkyPostView] = [:]

    func get(_ uri: String) -> BSkyPostView? { storage[uri] }
    func set(_ uri: String, _ post: BSkyPostView) { storage[uri] = post }
}

/// Fetches Bluesky posts from the public API.
enum BSkyPostFetcher {
    private static let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "BSkyEmbed")
    private static let baseURL = sharedPublicBskyApi()
    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 10
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config)
    }()

    static func fetchPosts(uris: [String]) async -> [BSkyPostView] {
        let cache = BSkyPostCache.shared
        var results: [BSkyPostView] = []
        var uncached: [String] = []

        for uri in uris {
            if let cached = await cache.get(uri) {
                results.append(cached)
            } else {
                uncached.append(uri)
            }
        }

        guard !uncached.isEmpty else { return results }

        let queryItems = uncached.map { URLQueryItem(name: "uris", value: $0) }
        guard var components = URLComponents(string: "\(baseURL)\(sharedXrpcFeedGetPosts())") else {
            return results
        }
        components.queryItems = queryItems

        guard let url = components.url else { return results }

        do {
            let (data, _) = try await JSONSafety.boundedData(from: url, using: session)
            try JSONSafety.validateResponse(data)
            let response = try JSONDecoder().decode(GetPostsResponse.self, from: data)

            for post in response.posts {
                await cache.set(post.uri, post)
                results.append(post)
            }
        } catch {
            logger.error("[BSkyPostFetcher] fetch failed: \(error.localizedDescription)")
        }

        return results
    }
}
