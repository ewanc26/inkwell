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

        if let blobContainer = try? container.decodeIfPresent(ComAtprotoLexicon.Repository.BlobContainer.self, forKey: .blob) {
            self.blob = blobContainer.blob
        } else {
            self.blob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blob)
        }
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
    public let content: [PcktBlock]?
    public let children: [PcktListItem]?
    public let attrs: PcktBlockAttrs?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        language: String? = nil,
        facets: [LeafletFacet]? = nil,
        content: [PcktBlock]? = nil,
        children: [PcktListItem]? = nil,
        attrs: PcktBlockAttrs? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.language = language
        self.facets = facets
        self.content = content
        self.children = children
        self.attrs = attrs
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        self.level = try container.decodeIfPresent(Int.self, forKey: .level)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.content = try container.decodeIfPresent([PcktBlock].self, forKey: .content)
        self.children = try container.decodeIfPresent([PcktListItem].self, forKey: .children)
        self.attrs = try container.decodeIfPresent(PcktBlockAttrs.self, forKey: .attrs)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(plaintext, forKey: .plaintext)
        try container.encodeIfPresent(level, forKey: .level)
        try container.encodeIfPresent(language, forKey: .language)
        try container.encodeIfPresent(facets, forKey: .facets)
        try container.encodeIfPresent(content, forKey: .content)
        try container.encodeIfPresent(children, forKey: .children)
        try container.encodeIfPresent(attrs, forKey: .attrs)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext
        case level
        case language
        case facets
        case content
        case children
        case attrs
    }
}

public struct PcktBlockAttrs: Codable, Equatable, Hashable, Sendable {
    public let blob: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let url: String?
    public let alt: String?

    public init(blob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil, url: String? = nil, alt: String? = nil) {
        self.blob = blob
        self.url = url
        self.alt = alt
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.url = try container.decodeIfPresent(String.self, forKey: .url)
        self.alt = try container.decodeIfPresent(String.self, forKey: .alt)

        if let blobContainer = try? container.decodeIfPresent(ComAtprotoLexicon.Repository.BlobContainer.self, forKey: .blob) {
            self.blob = blobContainer.blob
        } else {
            self.blob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blob)
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(url, forKey: .url)
        try container.encodeIfPresent(alt, forKey: .alt)
        try container.encodeIfPresent(blob, forKey: .blob)
    }

    enum CodingKeys: String, CodingKey {
        case blob
        case url
        case alt
    }
}

public struct PcktListItem: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let content: [PcktBlock]?

    public init(type: String, content: [PcktBlock]? = nil) {
        self.type = type
        self.content = content
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case content
    }
}
