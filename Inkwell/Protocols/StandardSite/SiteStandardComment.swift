//
//  SiteStandardComment.swift
//  Inkwell
//
//  Created on 22/06/2026.
//
//  The `pub.leaflet.comment` lexicon — comments on standard.site documents
//  stored as AT Protocol records. Matching the schema used by Leaflet and
//  standard.horse.
//

import Foundation
import ATProtoKit

/// A `pub.leaflet.comment` record — a comment on a standard.site document.
/// Supports threaded replies via the `reply` field, and optional richtext
/// facets on the `plaintext`.
///
/// - SeeAlso: standard.horse: lexicons/api/types/pub/leaflet/comment.ts
public struct PubLeafletComment: ATRecordProtocol, Sendable {

    /// The identifier of the lexicon.
    public static let type: String = "pub.leaflet.comment"

    /// AT-URI of the document being commented on.
    public let subject: String

    /// When the comment was created.
    public let createdAt: Date

    /// The plain text body of the comment.
    public let plaintext: String

    /// Optional richtext facets (byte-range annotations for links, mentions, etc.).
    public let facets: [LeafletFacet]?

    /// When this is a reply, references the parent comment.
    public let reply: ReplyRef?

    /// Optional page anchor within the document.
    public let onPage: String?

    public init(
        subject: String,
        createdAt: Date = Date(),
        plaintext: String,
        facets: [LeafletFacet]? = nil,
        reply: ReplyRef? = nil,
        onPage: String? = nil
    ) {
        self.subject = subject
        self.createdAt = createdAt
        self.plaintext = plaintext
        self.facets = facets
        self.reply = reply
        self.onPage = onPage
    }

    public init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.subject = try container.decode(String.self, forKey: .subject)
        self.createdAt = try container.decodeDate(forKey: .createdAt)
        self.plaintext = try container.decode(String.self, forKey: .plaintext)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.reply = try container.decodeIfPresent(ReplyRef.self, forKey: .reply)
        self.onPage = try container.decodeIfPresent(String.self, forKey: .onPage)
    }

    public func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encode(self.subject, forKey: .subject)
        try container.encodeDate(self.createdAt, forKey: .createdAt)
        try container.encode(self.plaintext, forKey: .plaintext)
        try container.encodeIfPresent(self.facets, forKey: .facets)
        try container.encodeIfPresent(self.reply, forKey: .reply)
        try container.encodeIfPresent(self.onPage, forKey: .onPage)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case subject
        case createdAt
        case plaintext
        case facets
        case reply
        case onPage
    }
}

extension PubLeafletComment {

    /// References a parent comment for threaded replies.
    /// Matching `pub.leaflet.comment#replyRef`.
    public struct ReplyRef: Sendable, Codable, Equatable, Hashable {
        /// AT-URI of the parent comment.
        public let parent: String

        public init(parent: String) {
            self.parent = parent
        }
    }
}
