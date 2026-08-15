//
//  SiteStandardPublication.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon {

    /// A record model for a Standard.site publication.
    ///
    /// A publication represents a collection of documents published to the web — the "site"
    /// itself, roughly analogous to an RSS `<channel>`. It's optional (a document can stand
    /// alone via its `site` URL), but recommended whenever documents belong to a
    /// larger collection.
    ///
    /// - SeeAlso: This is based on the [`site.standard.publication`][docs] lexicon.
    ///
    /// [docs]: https://standard.site/docs/lexicons/publication/
    public struct PublicationRecord: ATRecordProtocol, Sendable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.publication"

        /// The base URL for the publication (ex: `https://standard.site`).
        ///
        /// Combined with a document's `path` to construct that document's canonical URL.
        /// Avoid a trailing slash.
        public let url: String

        /// The name of the publication.
        ///
        /// - Important: Current maximum length is 500 graphemes (5,000 bytes).
        public let name: String

        /// A square image identifying the publication. Optional.
        ///
        /// Should be at least 256x256. Maximum size is 1MB.
        public let icon: ComAtprotoLexicon.Repository.UploadBlobOutput?

        /// A brief description of the publication. Optional.
        ///
        /// - Important: Current maximum length is 3,000 graphemes (30,000 bytes).
        public let description: String?

        /// The timestamp when the publication record was created. Optional.
        public let createdAt: Date?

        /// A simplified theme for tools and apps to use when displaying this publication's
        /// content. Optional.
        public let basicTheme: SiteStandardLexicon.Theme.BasicDefinition?

        /// Rich theme used by Leaflet and other compatible publishers.
        public let theme: SiteStandardLexicon.Theme.PublicationTheme?

        /// Self-label values for this publication — effectively content warnings. Optional.
        public let labels: ComAtprotoLexicon.Label.SelfLabelsDefinition?

        /// Platform-specific preferences for the publication. Optional.
        public let preferences: Preferences?

        public init(
            url: String,
            name: String,
            icon: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
            description: String? = nil,
            basicTheme: SiteStandardLexicon.Theme.BasicDefinition? = nil,
            theme: SiteStandardLexicon.Theme.PublicationTheme? = nil,
            labels: ComAtprotoLexicon.Label.SelfLabelsDefinition? = nil,
            preferences: Preferences? = nil,
            createdAt: Date? = nil
        ) {
            self.url = url
            self.name = name
            self.icon = icon
            self.description = description
            self.basicTheme = basicTheme
            self.theme = theme
            self.labels = labels
            self.preferences = preferences
            self.createdAt = createdAt
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.url = try container.decode(String.self, forKey: .url)
            self.name = try container.decode(String.self, forKey: .name)
            self.icon = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .icon)
            self.description = try container.decodeIfPresent(String.self, forKey: .description)
            self.basicTheme = try container.decodeIfPresent(SiteStandardLexicon.Theme.BasicDefinition.self, forKey: .basicTheme)
            self.theme = try container.decodeIfPresent(SiteStandardLexicon.Theme.PublicationTheme.self, forKey: .theme)
            self.labels = try container.decodeIfPresent(ComAtprotoLexicon.Label.SelfLabelsDefinition.self, forKey: .labels)
            self.preferences = try container.decodeIfPresent(Preferences.self, forKey: .preferences)
            self.createdAt = try container.decodeDateIfPresent(forKey: .createdAt)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            // Standard.site's own examples always include `$type` on the wire, so this is
            // written explicitly here rather than leaning on whatever's calling encode(to:)
            // to add it separately.
            try container.encode(SiteStandardLexicon.PublicationRecord.type, forKey: .type)
            try container.encode(self.url, forKey: .url)
            try container.truncatedEncode(self.name, forKey: .name, upToCharacterLength: 500)
            try container.encodeIfPresent(self.icon, forKey: .icon)
            try container.truncatedEncodeIfPresent(self.description, forKey: .description, upToCharacterLength: 3_000)
            try container.encodeIfPresent(self.basicTheme, forKey: .basicTheme)
            try container.encodeIfPresent(self.theme, forKey: .theme)
            try container.encodeIfPresent(self.labels, forKey: .labels)
            try container.encodeIfPresent(self.preferences, forKey: .preferences)
            try container.encodeDateIfPresent(self.createdAt, forKey: .createdAt)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case url
            case name
            case icon
            case description
            case basicTheme
            case theme
            case labels
            case preferences
            case createdAt
        }
    }
}

extension SiteStandardLexicon.PublicationRecord {

    /// Platform-specific preferences for a publication.
    ///
    /// This is an inline `object` type (not a separate `ref`), so unlike
    /// ``PublicationRecord`` itself, instances of this type don't carry a `$type`
    /// discriminator when encoded — matching every example in the Standard.site docs, which
    /// show `"preferences": { "showInDiscover": true }` with no `$type` inside it.
    ///
    /// - SeeAlso: This is based on the [`site.standard.publication#preferences`][docs] definition.
    ///
    /// [docs]: https://standard.site/docs/lexicons/publication/
    nonisolated public struct Preferences: Sendable, Codable, Equatable, Hashable {

        /// Whether the publication should appear in discovery feeds. Optional.
        public let showInDiscover: Bool?

        public init(showInDiscover: Bool? = nil) {
            self.showInDiscover = showInDiscover
        }
    }
}
