//
//  SiteStandardDocument.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon {

    /// A record model for a Standard.site document.
    ///
    /// Documents may be standalone or associated with a publication via ``site``. This is the
    /// record type behind individual blog posts/articles — roughly analogous to an RSS
    /// `<item>`.
    ///
    /// - SeeAlso: This is based on the [`site.standard.document`][docs] lexicon.
    ///
    /// [docs]: https://standard.site/docs/lexicons/document/
    public struct DocumentRecord: ATRecordProtocol, Sendable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.document"

        /// Points to a publication record (`at://...`) or, for loose/standalone documents, a
        /// publication URL (`https://...`).
        ///
        /// Avoid a trailing slash.
        public let site: String

        /// The title of the document.
        ///
        /// - Important: Current maximum length is 500 graphemes (5,000 bytes).
        public let title: String

        /// The timestamp of the document's publish time.
        public let publishedAt: Date

        /// Combined with ``site`` (or the publication's `url`) to construct a canonical URL to
        /// the document. Optional. Should be prefixed with a leading slash.
        public let path: String?

        /// A brief description or excerpt from the document. Optional.
        ///
        /// - Important: Current maximum length is 3,000 graphemes (30,000 bytes).
        public let description: String?

        /// An image used as a thumbnail or cover image. Optional. Maximum size is 1MB.
        public let coverImage: ComAtprotoLexicon.Repository.UploadBlobOutput?

        /// The record's content. Optional.
        ///
        /// This is an open union: Standard.site only defines the metadata layer and
        /// deliberately leaves content format up to each platform (Markdown, blocks, rich
        /// text, etc.). Represented here as `UnknownType` so any registered `$type` decodes
        /// into its concrete model via `ATRecordTypeRegistry`, while anything unregistered
        /// still safely round-trips as raw JSON instead of failing to decode.
        public let content: UnknownType?

        /// A plaintext representation of the document's contents. Optional.
        ///
        /// Should not contain Markdown or other formatting — useful for search/indexing
        /// without needing to understand ``content``'s format.
        public let textContent: String?

        /// Theme for standalone documents. Publication documents inherit the
        /// publication theme unless they provide this override.
        public let theme: SiteStandardLexicon.Theme.PublicationTheme?

        /// A strong reference to a Bluesky post. Optional.
        ///
        /// Useful for keeping track of comments/discussion happening off-platform, on Bluesky.
        public let bskyPostRef: ComAtprotoLexicon.Repository.StrongReference?

        /// Tags used to categorize the document. Optional.
        ///
        /// Avoid prepending tags with hashtags.
        ///
        /// - Important: Current maximum length for each tag is 128 graphemes (1,280 bytes).
        public let tags: [String]?

        /// Describes relationships between this document and external resources. Optional.
        ///
        /// Another open union, same rationale as ``content``. Standard.site's published docs
        /// don't detail a concrete schema for this beyond "open union," so it's left as
        /// `UnknownType` here too — extend with a registered type if/when a concrete shape
        /// is needed.
        public let links: UnknownType?

        /// Self-label values for this document — effectively content warnings. Optional.
        public let labels: ComAtprotoLexicon.Label.SelfLabelsDefinition?

        /// Additional contributors to this document, beyond the record's author. Optional.
        public let contributors: [Contributor]?

        /// The timestamp of the document's last edit. Optional.
        public let updatedAt: Date?

        public init(
            site: String,
            title: String,
            publishedAt: Date,
            path: String? = nil,
            description: String? = nil,
            coverImage: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
            content: UnknownType? = nil,
            textContent: String? = nil,
            theme: SiteStandardLexicon.Theme.PublicationTheme? = nil,
            bskyPostRef: ComAtprotoLexicon.Repository.StrongReference? = nil,
            tags: [String]? = nil,
            links: UnknownType? = nil,
            labels: ComAtprotoLexicon.Label.SelfLabelsDefinition? = nil,
            contributors: [Contributor]? = nil,
            updatedAt: Date? = nil
        ) {
            self.site = site
            self.title = title
            self.publishedAt = publishedAt
            self.path = path
            self.description = description
            self.coverImage = coverImage
            self.content = content
            self.textContent = textContent
            self.theme = theme
            self.bskyPostRef = bskyPostRef
            self.tags = tags
            self.links = links
            self.labels = labels
            self.contributors = contributors
            self.updatedAt = updatedAt
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.site = try container.decode(String.self, forKey: .site)
            self.title = try container.decode(String.self, forKey: .title)
            self.publishedAt = try container.decodeDate(forKey: .publishedAt)
            self.path = try container.decodeIfPresent(String.self, forKey: .path)
            self.description = try container.decodeIfPresent(String.self, forKey: .description)
            self.coverImage = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .coverImage)
            self.content = try container.decodeIfPresent(UnknownType.self, forKey: .content)
            self.textContent = try container.decodeIfPresent(String.self, forKey: .textContent)
            self.theme = try container.decodeIfPresent(SiteStandardLexicon.Theme.PublicationTheme.self, forKey: .theme)
            self.bskyPostRef = try container.decodeIfPresent(ComAtprotoLexicon.Repository.StrongReference.self, forKey: .bskyPostRef)
            self.tags = try container.decodeIfPresent([String].self, forKey: .tags)
            self.links = try container.decodeIfPresent(UnknownType.self, forKey: .links)
            self.labels = try container.decodeIfPresent(ComAtprotoLexicon.Label.SelfLabelsDefinition.self, forKey: .labels)
            self.contributors = try container.decodeIfPresent([Contributor].self, forKey: .contributors)
            self.updatedAt = try container.decodeDateIfPresent(forKey: .updatedAt)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(SiteStandardLexicon.DocumentRecord.type, forKey: .type)
            try container.encode(self.site, forKey: .site)
            try container.truncatedEncode(self.title, forKey: .title, upToCharacterLength: 500)
            try container.encodeDate(self.publishedAt, forKey: .publishedAt)
            try container.encodeIfPresent(self.path, forKey: .path)
            try container.truncatedEncodeIfPresent(self.description, forKey: .description, upToCharacterLength: 3_000)
            try container.encodeIfPresent(self.coverImage, forKey: .coverImage)
            try container.encodeIfPresent(self.content, forKey: .content)
            try container.encodeIfPresent(self.textContent, forKey: .textContent)
            try container.encodeIfPresent(self.theme, forKey: .theme)
            try container.encodeIfPresent(self.bskyPostRef, forKey: .bskyPostRef)
            try container.truncatedEncodeIfPresent(self.tags, forKey: .tags, upToCharacterLength: 128)
            try container.encodeIfPresent(self.links, forKey: .links)
            try container.encodeIfPresent(self.labels, forKey: .labels)
            try container.encodeIfPresent(self.contributors, forKey: .contributors)
            try container.encodeDateIfPresent(self.updatedAt, forKey: .updatedAt)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case site
            case title
            case publishedAt
            case path
            case description
            case coverImage
            case content
            case textContent
            case theme
            case bskyPostRef
            case tags
            case links
            case labels
            case contributors
            case updatedAt
        }
    }
}

extension SiteStandardLexicon.DocumentRecord {

    /// Describes a participant on a document beyond the record's author.
    ///
    /// - SeeAlso: This is based on the [`site.standard.document#contributor`][docs] definition.
    ///
    /// [docs]: https://standard.site/docs/lexicons/document/
    public struct Contributor: Sendable, Codable, Equatable, Hashable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.document#contributor"

        /// The decentralized identifier (DID) of the contributor.
        public let did: String

        /// The contributor's role (ex: "editor", "translator"). Optional.
        ///
        /// - Important: Current maximum length is 100 graphemes (1,000 bytes).
        public let role: String?

        /// An optional display name override for the contributor. Optional.
        ///
        /// - Important: Current maximum length is 100 graphemes (1,000 bytes).
        public let displayName: String?

        public init(did: String, role: String? = nil, displayName: String? = nil) {
            self.did = did
            self.role = role
            self.displayName = displayName
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.did = try container.decode(String.self, forKey: .did)
            self.role = try container.decodeIfPresent(String.self, forKey: .role)
            self.displayName = try container.decodeIfPresent(String.self, forKey: .displayName)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(Contributor.type, forKey: .type)
            try container.encode(self.did, forKey: .did)
            try container.truncatedEncodeIfPresent(self.role, forKey: .role, upToCharacterLength: 100)
            try container.truncatedEncodeIfPresent(self.displayName, forKey: .displayName, upToCharacterLength: 100)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case did
            case role
            case displayName
        }
    }
}
