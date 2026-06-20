//
//  LeafletContent.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  The `pub.leaflet.content` record format — what Leaflet (leaflet.pub)
//  itself stores in a standard.site document's `content` open union.
//  Not part of standard.site proper; registered separately by
//  ContentFormatRegistration.swift so `UnknownType` can resolve it, and
//  converted to/from the editor's markdown by `LeafletProvider` in
//  ContentProvider.swift.
//

import Foundation
import ATProtoKit

// MARK: - pub.leaflet.content

public struct LeafletContent: ATRecordProtocol {
    public static private(set) var type = "pub.leaflet.content"

    public let pages: [LeafletPage]?
    public let blobPages: ComAtprotoLexicon.Repository.UploadBlobOutput?

    public init(pages: [LeafletPage]?, blobPages: ComAtprotoLexicon.Repository.UploadBlobOutput?) {
        self.pages = pages
        self.blobPages = blobPages
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.pages = try container.decodeIfPresent([LeafletPage].self, forKey: .pages)

        if let blobContainer = try? container.decodeIfPresent(ComAtprotoLexicon.Repository.BlobContainer.self, forKey: .blobPages) {
            self.blobPages = blobContainer.blob
        } else {
            self.blobPages = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blobPages)
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encodeIfPresent(pages, forKey: .pages)
        try container.encodeIfPresent(blobPages, forKey: .blobPages)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case pages
        case blobPages
    }
}

public struct LeafletPage: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let blocks: [LeafletBlockContainer]?

    public init(type: String, blocks: [LeafletBlockContainer]?) {
        self.type = type
        self.blocks = blocks
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case blocks
    }
}

public struct LeafletBlockContainer: Codable, Equatable, Hashable, Sendable {
    public let block: LeafletBlock
    public let alignment: String?

    public init(block: LeafletBlock, alignment: String? = nil) {
        self.block = block
        self.alignment = alignment
    }
}

public struct LeafletBlock: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let plaintext: String?
    public let level: Int?
    public let language: String?
    public let tex: String?
    public let image: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let alt: String?
    public let facets: [LeafletFacet]?

    // Lists
    public let children: [LeafletListItem]?
    public let startIndex: Int?
    public let orderedListChildren: IndirectBox<LeafletBlockContainer>?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        language: String? = nil,
        tex: String? = nil,
        image: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        alt: String? = nil,
        facets: [LeafletFacet]? = nil,
        children: [LeafletListItem]? = nil,
        startIndex: Int? = nil,
        orderedListChildren: IndirectBox<LeafletBlockContainer>? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.language = language
        self.tex = tex
        self.image = image
        self.alt = alt
        self.facets = facets
        self.children = children
        self.startIndex = startIndex
        self.orderedListChildren = orderedListChildren
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        self.level = try container.decodeIfPresent(Int.self, forKey: .level)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.tex = try container.decodeIfPresent(String.self, forKey: .tex)

        if let imageContainer = try? container.decodeIfPresent(ComAtprotoLexicon.Repository.BlobContainer.self, forKey: .image) {
            self.image = imageContainer.blob
        } else {
            self.image = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .image)
        }

        self.alt = try container.decodeIfPresent(String.self, forKey: .alt)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.children = try container.decodeIfPresent([LeafletListItem].self, forKey: .children)
        self.startIndex = try container.decodeIfPresent(Int.self, forKey: .startIndex)

        // Handle potential circular list structure
        self.orderedListChildren = try container.decodeIfPresent(IndirectBox<LeafletBlockContainer>.self, forKey: .orderedListChildren)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(plaintext, forKey: .plaintext)
        try container.encodeIfPresent(level, forKey: .level)
        try container.encodeIfPresent(language, forKey: .language)
        try container.encodeIfPresent(tex, forKey: .tex)
        try container.encodeIfPresent(image, forKey: .image)
        try container.encodeIfPresent(alt, forKey: .alt)
        try container.encodeIfPresent(facets, forKey: .facets)
        try container.encodeIfPresent(children, forKey: .children)
        try container.encodeIfPresent(startIndex, forKey: .startIndex)
        try container.encodeIfPresent(orderedListChildren, forKey: .orderedListChildren)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext
        case level
        case language
        case tex
        case image
        case alt
        case facets
        case children
        case startIndex
        case orderedListChildren
    }
}

public struct LeafletListItem: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let content: LeafletBlock?
    public let checked: Bool?
    public let children: [LeafletListItem]?
    public let orderedListChildren: IndirectBox<LeafletBlockContainer>?

    public init(
        type: String,
        content: LeafletBlock? = nil,
        checked: Bool? = nil,
        children: [LeafletListItem]? = nil,
        orderedListChildren: IndirectBox<LeafletBlockContainer>? = nil
    ) {
        self.type = type
        self.content = content
        self.checked = checked
        self.children = children
        self.orderedListChildren = orderedListChildren
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case content
        case checked
        case children
        case orderedListChildren
    }
}
