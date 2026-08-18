//
//  UserInputFeedback.swift
//  Inkwell
//
//  Minimal support for posting in-app feedback to userinput.app
//  (https://userinput.app), a federated feedback board built on AT
//  Protocol. Inkwell only ever *creates* app.userinput.discussion records
//  in the signed-in user's own repo, pointing at Inkwell's own feedback
//  space (owned by ewancroft.uk) via a strong reference — it doesn't
//  implement the rest of userinput.app's lexicon surface (voting,
//  moderation, replies, etc.), which isn't needed for a "send feedback" flow.
//

import Foundation
import ATProtoKit

/// `app.userinput.discussion` — a piece of feedback posted into a space.
/// Lives in the author's repo and points at the space.
struct UserInputDiscussionRecord: ATRecordProtocol, Sendable, Equatable, Hashable {
    static let type: String = "app.userinput.discussion"

    let title: String
    let body: String?
    let tags: [String]?
    let space: SpaceReference
    let createdAt: Date

    /// A `com.atproto.repo.strongRef`-shaped reference to the target space.
    /// Deliberately doesn't include `$type` — matches the wire shape
    /// userinput.app's own `space` field expects (a plain `ref`, not a
    /// union), and Inkwell's Android client for consistency.
    struct SpaceReference: Codable, Sendable, Equatable, Hashable {
        let uri: String
        let cid: String
    }

    init(title: String, body: String?, tags: [String]?, space: SpaceReference, createdAt: Date = Date()) {
        self.title = title
        self.body = body
        self.tags = tags
        self.space = space
        self.createdAt = createdAt
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        title = try container.decode(String.self, forKey: .title)
        body = try container.decodeIfPresent(String.self, forKey: .body)
        tags = try container.decodeIfPresent([String].self, forKey: .tags)
        space = try container.decode(SpaceReference.self, forKey: .space)
        createdAt = try container.decode(Date.self, forKey: .createdAt)
    }

    func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(UserInputDiscussionRecord.type, forKey: .type)
        try container.encode(title, forKey: .title)
        try container.encodeIfPresent(body, forKey: .body)
        try container.encodeIfPresent(tags, forKey: .tags)
        try container.encode(space, forKey: .space)
        try container.encode(createdAt, forKey: .createdAt)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case title, body, tags, space, createdAt
    }
}

/// Constants for Inkwell's feedback flow. Mirrors the shared KMP
/// `UserInputLexicon` (Android consumes that directly; iOS keeps its own
/// copy here since the discussion record itself is a typed Swift/ATProtoKit
/// model, not something that round-trips through the shared JSON bridge).
enum UserInputFeedback {
    /// Inkwell's own feedback space, owned by ewancroft.uk.
    static let inkwellSpaceURI = "at://did:plc:ofrbh253gwicbkc5nktqepol/app.userinput.space/3mtdoxmi2lp27"

    /// Tag values defined on Inkwell's feedback space, in display order.
    static let tags = ["bug", "question", "ios", "android", "altstore", "f-droid"]

    static let titleMaxLength = 600
    static let bodyMaxLength = 20_000
}
