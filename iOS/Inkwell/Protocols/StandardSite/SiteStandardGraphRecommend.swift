//
//  SiteStandardGraphRecommend.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon.Graph {

    /// A record model for a Standard.site recommend.
    ///
    /// A lightweight social signal: a user creates one of these pointing at a
    /// ``SiteStandardLexicon/DocumentRecord`` they endorse. Aggregators and readers can use
    /// these to surface popular or trusted documents — unlike ``SubscriptionRecord``, which
    /// follows a whole publication, this endorses a single document.
    ///
    /// - SeeAlso: This is based on the [`site.standard.graph.recommend`][docs] lexicon.
    ///
    /// [docs]: https://standard.site/docs/lexicons/recommend/
    public struct RecommendRecord: ATRecordProtocol, Sendable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.graph.recommend"

        /// An AT-URI reference to the document record being recommended
        /// (ex: `at://did:plc:abc123/site.standard.document/xyz789`).
        public let document: String

        /// The timestamp marking when the recommend was created.
        public let createdAt: Date

        public init(document: String, createdAt: Date) {
            self.document = document
            self.createdAt = createdAt
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.document = try container.decode(String.self, forKey: .document)
            self.createdAt = try container.decodeDate(forKey: .createdAt)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(SiteStandardLexicon.Graph.RecommendRecord.type, forKey: .type)
            try container.encode(self.document, forKey: .document)
            try container.encodeDate(self.createdAt, forKey: .createdAt)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case document
            case createdAt
        }
    }
}
