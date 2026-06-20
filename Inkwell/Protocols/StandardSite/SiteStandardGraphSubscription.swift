//
//  SiteStandardGraphSubscription.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon.Graph {

    /// A record model for a Standard.site subscription.
    ///
    /// Tracks a relationship between a user and a publication, enabling follow-style
    /// functionality and personalised content feeds across the network. The subscriber is
    /// whoever's repo the record lives in — there's no separate "subscriber" field, same as
    /// `app.bsky.graph.follow`.
    ///
    /// - SeeAlso: This is based on the [`site.standard.graph.subscription`][docs] lexicon.
    ///
    /// [docs]: https://standard.site/docs/lexicons/subscription/
    public struct SubscriptionRecord: ATRecordProtocol, Sendable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.graph.subscription"

        /// An AT-URI reference to the publication record being subscribed to
        /// (ex: `at://did:plc:abc123/site.standard.publication/xyz789`).
        public let publication: String

        /// The timestamp marking when the subscription was created. Optional.
        public let createdAt: Date?

        public init(publication: String, createdAt: Date? = nil) {
            self.publication = publication
            self.createdAt = createdAt
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.publication = try container.decode(String.self, forKey: .publication)
            self.createdAt = try container.decodeDateIfPresent(forKey: .createdAt)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(SiteStandardLexicon.Graph.SubscriptionRecord.type, forKey: .type)
            try container.encode(self.publication, forKey: .publication)
            try container.encodeDateIfPresent(self.createdAt, forKey: .createdAt)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case publication
            case createdAt
        }
    }
}
