//
//  OffprintContent.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  The `app.offprint.content` record format — offprint.app's block-array
//  content shape. Converted to/from the editor's markdown by
//  `OffprintProvider` in ContentProvider.swift; inline formatting uses
//  ``LeafletFacet`` (see RichTextFacets.swift) under offprint's own
//  `app.offprint.richtext.facet#*` `$type` strings.
//

import Foundation
import ATProtoKit

// MARK: - app.offprint.content

public struct OffprintContent: ATRecordProtocol {
    public static private(set) var type = "app.offprint.content"

    public let items: [OffprintBlock]?

    public init(items: [OffprintBlock]? = nil) {
        self.items = items
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.items = try container.decodeIfPresent([OffprintBlock].self, forKey: .items)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encodeIfPresent(items, forKey: .items)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case items
    }
}

public struct OffprintBlock: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let plaintext: String?
    public let level: Int?
    public let language: String?
    public let facets: [LeafletFacet]?
    public let content: [OffprintBlock]?
    public let image: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let alt: String?

    // Lists
    public let children: [OffprintListItem]?
    public let ordered: Bool?
    public let start: Int?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        language: String? = nil,
        facets: [LeafletFacet]? = nil,
        content: [OffprintBlock]? = nil,
        image: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        alt: String? = nil,
        children: [OffprintListItem]? = nil,
        ordered: Bool? = nil,
        start: Int? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.language = language
        self.facets = facets
        self.content = content
        self.image = image
        self.alt = alt
        self.children = children
        self.ordered = ordered
        self.start = start
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        self.level = try container.decodeIfPresent(Int.self, forKey: .level)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.content = try container.decodeIfPresent([OffprintBlock].self, forKey: .content)
        self.alt = try container.decodeIfPresent(String.self, forKey: .alt)

        if let imageContainer = try? container.decodeIfPresent(ComAtprotoLexicon.Repository.BlobContainer.self, forKey: .image) {
            self.image = imageContainer.blob
        } else {
            self.image = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .image)
        }

        self.children = try container.decodeIfPresent([OffprintListItem].self, forKey: .children)
        self.ordered = try container.decodeIfPresent(Bool.self, forKey: .ordered)
        self.start = try container.decodeIfPresent(Int.self, forKey: .start)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(plaintext, forKey: .plaintext)
        try container.encodeIfPresent(level, forKey: .level)
        try container.encodeIfPresent(language, forKey: .language)
        try container.encodeIfPresent(facets, forKey: .facets)
        try container.encodeIfPresent(content, forKey: .content)
        try container.encodeIfPresent(image, forKey: .image)
        try container.encodeIfPresent(alt, forKey: .alt)
        try container.encodeIfPresent(children, forKey: .children)
        try container.encodeIfPresent(ordered, forKey: .ordered)
        try container.encodeIfPresent(start, forKey: .start)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext
        case level
        case language
        case facets
        case content
        case image
        case alt
        case children
        case ordered
        case start
    }
}

public struct OffprintListItem: Codable, Equatable, Hashable, Sendable {
    public let type: String
    /// A single block (almost always `app.offprint.block.text`) — offprint's
    /// real schema stores one item body here, not an array. See
    /// standard.horse's `offprint.ts` `itemBlock`/`itemToMdast`.
    public let content: OffprintBlock?
    public let checked: Bool?
    /// Nested sub-list items, for a sub-list directly under this item.
    public let children: [OffprintListItem]?

    public init(
        type: String,
        content: OffprintBlock? = nil,
        checked: Bool? = nil,
        children: [OffprintListItem]? = nil
    ) {
        self.type = type
        self.content = content
        self.checked = checked
        self.children = children
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case content
        case checked
        case children
    }
}
