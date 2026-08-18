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

    struct BSkyExternal: Decodable, Sendable {
        let uri: String?
        let title: String?
        let description: String?
        let thumb: String?
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
            let (data, _) = try await session.data(from: url)
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
