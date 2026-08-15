//
//  PcktContent.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  The `blog.pckt.content` record format — pckt.blog's block-array content
//  shape. Converted to/from the editor's markdown by `PcktProvider` in
//  ContentProvider.swift; inline formatting uses ``LeafletFacet`` (see
//  RichTextFacets.swift) under pckt's own `blog.pckt.richtext.facet#*`
//  `$type` strings.
//

import Foundation
import ATProtoKit

// MARK: - blog.pckt.content

public struct PcktContent: ATRecordProtocol {
    public static private(set) var type = "blog.pckt.content"

    public let items: [PcktBlock]?
    public let blob: ComAtprotoLexicon.Repository.UploadBlobOutput?

    public init(items: [PcktBlock]? = nil, blob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil) {
        self.items = items
        self.blob = blob
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.items = try container.decodeIfPresent([PcktBlock].self, forKey: .items)
        self.blob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blob)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encodeIfPresent(items, forKey: .items)
        try container.encodeIfPresent(blob, forKey: .blob)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case items
        case blob
    }
}

public struct PcktBlock: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let plaintext: String?
    public let level: Int?
    public let language: String?
    public let facets: [LeafletFacet]?
    /// Nested blocks, for a blockquote's inner paragraphs.
    public let content: [PcktBlock]?
    /// List items, for `bulletList`/`orderedList`/`taskList`. Pckt's real
    /// lexicon keys this under the *same* `content` field as the blockquote
    /// case above (not `children`) — see standard.horse's `pckt.ts`
    /// `listToMdast`/`listBlock`. Kept as a separate Swift property since the
    /// element shape differs (list items vs. nested blocks); `init(from:)`/
    /// `encode(to:)` below route both through the one JSON key by `$type`.
    public let listContent: [PcktListItem]?
    /// Start index for `orderedList`.
    public let start: Int?
    public let attrs: PcktBlockAttrs?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        language: String? = nil,
        facets: [LeafletFacet]? = nil,
        content: [PcktBlock]? = nil,
        listContent: [PcktListItem]? = nil,
        start: Int? = nil,
        attrs: PcktBlockAttrs? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.language = language
        self.facets = facets
        self.content = content
        self.listContent = listContent
        self.start = start
        self.attrs = attrs
    }

    private static let listBlockSuffixes = ["bulletList", "orderedList", "taskList"]

    private static func isListBlock(_ type: String) -> Bool {
        listBlockSuffixes.contains { type.hasSuffix($0) }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        self.level = try container.decodeIfPresent(Int.self, forKey: .level)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.start = try container.decodeIfPresent(Int.self, forKey: .start)
        self.attrs = try container.decodeIfPresent(PcktBlockAttrs.self, forKey: .attrs)

        // `content` is polymorphic in pckt's real lexicon: nested blocks for
        // a blockquote, list items for bulletList/orderedList/taskList.
        // Branch on `$type` rather than trying both shapes — a failed
        // array-of-X decode can otherwise partially succeed and produce
        // garbage instead of throwing.
        if Self.isListBlock(self.type) {
            self.listContent = try container.decodeIfPresent([PcktListItem].self, forKey: .content)
            self.content = nil
        } else {
            self.content = try container.decodeIfPresent([PcktBlock].self, forKey: .content)
            self.listContent = nil
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(plaintext, forKey: .plaintext)
        try container.encodeIfPresent(level, forKey: .level)
        try container.encodeIfPresent(language, forKey: .language)
        try container.encodeIfPresent(facets, forKey: .facets)
        try container.encodeIfPresent(start, forKey: .start)
        try container.encodeIfPresent(attrs, forKey: .attrs)
        if let listContent {
            try container.encode(listContent, forKey: .content)
        } else {
            try container.encodeIfPresent(content, forKey: .content)
        }
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext
        case level
        case language
        case facets
        case content
        case start
        case attrs
    }
}

public struct PcktBlockAttrs: Codable, Equatable, Hashable, Sendable {
    public let blob: ComAtprotoLexicon.Repository.UploadBlobOutput?
    /// pckt's actual attribute name for an image's source (CID-as-blob or
    /// an external URL) is `src`, not `url` — see standard.horse's
    /// `pckt.ts` `imageBlock`/`blockToMdast`.
    public let src: String?
    public let alt: String?

    public init(blob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil, src: String? = nil, alt: String? = nil) {
        self.blob = blob
        self.src = src
        self.alt = alt
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.src = try container.decodeIfPresent(String.self, forKey: .src)
        self.alt = try container.decodeIfPresent(String.self, forKey: .alt)

        self.blob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blob)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(src, forKey: .src)
        try container.encodeIfPresent(alt, forKey: .alt)
        try container.encodeIfPresent(blob, forKey: .blob)
    }

    enum CodingKeys: String, CodingKey {
        case blob
        case src
        case alt
    }
}

public struct PcktListItem: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let content: [PcktBlock]?
    /// Set for `taskItem`s only; absent for plain `listItem`s.
    public let checked: Bool?

    public init(type: String, content: [PcktBlock]? = nil, checked: Bool? = nil) {
        self.type = type
        self.content = content
        self.checked = checked
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case content
        case checked
    }
}
