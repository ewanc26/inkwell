//
//  BSkyProfileFetcher.swift
//  Inkwell
//
//  Fetches Bluesky actor profiles from the public Bluesky API
//  (public.api.bsky.app) so no authentication is needed.
//  Responses are cached in-memory for the lifetime of the process.
//

import Foundation
import OSLog

// MARK: - Profile Model

/// A full Bluesky actor profile returned by `app.bsky.actor.getProfile`.
struct BSkyActorProfile: Decodable, Sendable, Identifiable {
    let did: String
    let handle: String
    let displayName: String?
    let description: String?
    let avatar: String?
    let banner: String?
    let followersCount: Int?
    let followsCount: Int?
    let postsCount: Int?
    let createdAt: String?
    let indexedAt: String?
    let pinnedPost: BSkyPinnedPostRef?
    let labels: [BSkyLabel]?
    let associated: BSkyAssociated?
    let viewer: BSkyViewerState?

    var id: String { did }
}

struct BSkyPinnedPostRef: Decodable, Sendable {
    let cid: String?
    let uri: String?
}

struct BSkyLabel: Decodable, Sendable {
    let src: String?
    let uri: String?
    let val: String?
    let cts: String?
}

struct BSkyAssociated: Decodable, Sendable {
    let lists: Int?
    let feedgens: Int?
    let starterPacks: Int?
    let labeler: Bool?
    let chat: BSkyChatPreference?

    struct BSkyChatPreference: Decodable, Sendable {
        let allowIncoming: String?
    }
}

struct BSkyViewerState: Decodable, Sendable {
    let muted: Bool?
    let blockedBy: Bool?
    let blocking: String?
    let following: String?
    let followedBy: String?

    enum CodingKeys: String, CodingKey {
        case muted, blockedBy, blocking, following
        case followedBy
    }
}

// MARK: - API Response (resolveHandle → DID)

/// Response from `com.atproto.identity.resolveHandle`.
private struct ResolveHandleResponse: Decodable, Sendable {
    let did: String
}

// MARK: - Cache

/// Simple in-memory cache for fetched profiles.
private actor BSkyProfileCache {
    static let shared = BSkyProfileCache()
    private var storage: [String: BSkyActorProfile] = [:]

    func get(_ key: String) -> BSkyActorProfile? { storage[key] }
    func set(_ key: String, _ profile: BSkyActorProfile) { storage[key] = profile }
}

// MARK: - Fetcher

/// Fetches Bluesky actor profiles from the public API (no auth required).
enum BSkyProfileFetcher {
    private static let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "BSkyProfile")
    private static let baseURL = "https://public.api.bsky.app"
    private static let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 10
        config.requestCachePolicy = .returnCacheDataElseLoad
        return URLSession(configuration: config)
    }()

    // MARK: - Public API

    /// Fetches a profile by DID.
    ///
    /// This is the preferred method when you already have the DID
    /// (e.g. from an AT-URI or a stored identity).
    static func fetchProfile(did: String) async throws -> BSkyActorProfile {
        let cache = BSkyProfileCache.shared
        if let cached = await cache.get(did) {
            return cached
        }

        guard let url = URL(string: "\(baseURL)/xrpc/app.bsky.actor.getProfile?actor=\(did)") else {
            throw URLError(.badURL)
        }

        let (data, response) = try await session.data(from: url)

        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        guard (200...299).contains(http.statusCode) else {
            logger.error("[BSkyProfileFetcher] HTTP \(http.statusCode) fetching profile for \(did)")
            throw URLError(.badServerResponse)
        }

        let profile = try JSONDecoder().decode(BSkyActorProfile.self, from: data)
        await cache.set(did, profile)
        await cache.set(profile.handle.lowercased(), profile)
        return profile
    }

    /// Fetches a profile by handle (e.g. `alice.bsky.social`).
    ///
    /// This first resolves the handle to a DID via the public API,
    /// then fetches the full profile. Prefer `fetchProfile(did:)` if
    /// you already have the DID — it saves a network round-trip.
    static func fetchProfile(handle: String) async throws -> BSkyActorProfile {
        let cache = BSkyProfileCache.shared
        let normalised = handle.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "@", with: "")

        if let cached = await cache.get(normalised) {
            return cached
        }

        // Resolve handle → DID
        guard let resolveURL = URL(string: "\(baseURL)/xrpc/com.atproto.identity.resolveHandle?handle=\(normalised)") else {
            throw URLError(.badURL)
        }

        let (resolveData, resolveResponse) = try await session.data(from: resolveURL)
        guard let http = resolveResponse as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }

        let resolved = try JSONDecoder().decode(ResolveHandleResponse.self, from: resolveData)
        return try await fetchProfile(did: resolved.did)
    }
}
