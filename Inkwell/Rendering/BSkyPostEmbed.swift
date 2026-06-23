//
//  BSkyPostEmbed.swift
//  Inkwell
//
//  Fetches and renders embedded Bluesky posts (pub.leaflet.blocks.bskyPost).
//  Uses the public Bluesky API (public.api.bsky.app) so no authentication
//  is needed. Responses are cached in-memory for the lifetime of the view.
//

import SwiftUI
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
        case "app.bsky.embed.images":
            self = .images(try BSkyImagesEmbed(from: decoder))
        case "app.bsky.embed.external":
            self = .external(try BSkyExternalEmbed(from: decoder))
        case "app.bsky.embed.record":
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
    private static let baseURL = "https://public.api.bsky.app"
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
        guard var components = URLComponents(string: "\(baseURL)/xrpc/app.bsky.feed.getPosts") else {
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

// MARK: - Renderer

/// A live-rendered Bluesky post embed. Fetches the post from the public
/// Bluesky API on appearance and renders author info, post text, images,
/// external link cards, and quoted posts.
struct BSkyPostEmbedView: View {
    let postURI: String
    var foregroundColor: Color = .primary
    var accentColor: Color = .blue

    @State private var post: BSkyPostView?
    @State private var loadError: Bool = false

    var body: some View {
        Group {
            if let post {
                postContent(post)
            } else if loadError {
                fallbackCard
            } else {
                loadingCard
            }
        }
        .task {
            let posts = await BSkyPostFetcher.fetchPosts(uris: [postURI])
            if let first = posts.first {
                post = first
            } else {
                loadError = true
            }
        }
    }

    // MARK: - Post Content

    @ViewBuilder
    private func postContent(_ post: BSkyPostView) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            // Author row
            HStack(spacing: 10) {
                AsyncImage(url: URL(string: post.author.avatar ?? "")) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else {
                        Color.gray.opacity(0.2)
                    }
                }
                .frame(width: 28, height: 28)
                .clipShape(Circle())

                VStack(alignment: .leading, spacing: 1) {
                    Text(post.author.displayName ?? post.author.handle ?? "Unknown")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                    if let handle = post.author.handle {
                        Text("@\(handle)")
                            .font(.caption)
                            .foregroundStyle(foregroundColor.opacity(0.5))
                    }
                }
                Spacer()
            }

            // Post text
            if let text = post.record.text, !text.isEmpty {
                Text(text)
                    .font(.body)
                    .foregroundStyle(foregroundColor)
                    .lineSpacing(4)
            }

            // Embeds
            if let embed = post.embed {
                embedContent(embed)
            }

            // Stats row
            HStack(spacing: 20) {
                stat(icon: "bubble.right", count: post.replyCount)
                stat(icon: "arrow.2.squarepath", count: post.repostCount)
                stat(icon: "heart", count: post.likeCount)
                Spacer()
                Image(systemName: "bubble.left.fill")
                    .font(.caption)
                    .foregroundStyle(accentColor.opacity(0.6))
                Text("Bluesky")
                    .font(.caption2)
                    .foregroundStyle(foregroundColor.opacity(0.4))
            }
            .padding(.top, 4)
        }
        .padding(14)
        .background(foregroundColor.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
        )
    }

    // MARK: - Embed Content

    @ViewBuilder
    private func embedContent(_ embed: BSkyEmbed) -> some View {
        switch embed {
        case .images(let imagesEmbed):
            if let first = imagesEmbed.images.first, let thumb = first.thumb {
                AsyncImage(url: URL(string: thumb)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 200)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }

        case .external(let externalEmbed):
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    if let title = externalEmbed.external.title {
                        Text(title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(foregroundColor)
                            .lineLimit(2)
                    }
                    if let desc = externalEmbed.external.description {
                        Text(desc)
                            .font(.caption)
                            .foregroundStyle(foregroundColor.opacity(0.6))
                            .lineLimit(2)
                    }
                    if let uri = externalEmbed.external.uri {
                        Text(host(from: uri))
                            .font(.caption2)
                            .foregroundStyle(foregroundColor.opacity(0.4))
                    }
                }
                Spacer()
                if let thumb = externalEmbed.external.thumb {
                    AsyncImage(url: URL(string: thumb)) { phase in
                        if let image = phase.image {
                            image.resizable().scaledToFill()
                                .frame(width: 60, height: 60)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    }
                }
            }
            .padding(10)
            .background(foregroundColor.opacity(0.03))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(foregroundColor.opacity(0.08), lineWidth: 1)
            )

        case .record(let recordEmbed):
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: "quote.bubble")
                        .font(.caption)
                        .foregroundStyle(accentColor)
                    Text(recordEmbed.record.author?.displayName ?? "Quoted post")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                }
                if let text = recordEmbed.record.value?.text {
                    Text(text)
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.7))
                        .lineLimit(3)
                }
            }
            .padding(10)
            .background(foregroundColor.opacity(0.03))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(foregroundColor.opacity(0.08), lineWidth: 1)
            )

        case .unknown:
            EmptyView()
        }
    }

    // MARK: - Helpers

    private func stat(icon: String, count: Int?) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
            if let count {
                Text(formatCount(count))
                    .font(.caption2)
            }
        }
        .foregroundStyle(foregroundColor.opacity(0.4))
    }

    private func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 { return "\(count / 1_000_000)M" }
        if count >= 1_000 { return "\(count / 1_000)K" }
        return "\(count)"
    }

    private func host(from urlString: String) -> String {
        URL(string: urlString)?.host ?? urlString
    }

    // MARK: - States

    private var fallbackCard: some View {
        HStack(spacing: 8) {
            Image(systemName: "bubble.left.fill")
                .foregroundStyle(accentColor)
            Text("Bluesky post unavailable")
                .font(.subheadline)
                .foregroundStyle(foregroundColor.opacity(0.5))
            Spacer()
        }
        .padding(12)
        .background(foregroundColor.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
        )
    }

    private var loadingCard: some View {
        HStack(spacing: 10) {
            ProgressView()
                .scaleEffect(0.8)
            Text("Loading Bluesky post…")
                .font(.subheadline)
                .foregroundStyle(foregroundColor.opacity(0.5))
            Spacer()
        }
        .padding(14)
        .background(foregroundColor.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    VStack(spacing: 20) {
        BSkyPostEmbedView(
            postURI: "at://did:plc:example/app.bsky.feed.post/123",
            foregroundColor: .primary,
            accentColor: .blue
        )
        .padding()
    }
    .background(Color(.systemBackground))
}
