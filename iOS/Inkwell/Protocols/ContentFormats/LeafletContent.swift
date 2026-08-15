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

        self.blobPages = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blobPages)
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

/// A single block within a Leaflet page. Covers all 18 block types defined
/// by `pub.leaflet.blocks.*` — text, header, image, code, math, embeds,
/// polls, lists, and layout blocks.
///
/// Fields are named to match the AT Protocol lexicon JSON keys. A block only
/// populates the fields relevant to its `$type`; all others are nil.
public struct LeafletBlock: Codable, Equatable, Hashable, Sendable {

    // MARK: - All blocks
    public let type: String

    // MARK: - Text / header / blockquote / code
    public let plaintext: String?
    public let level: Int?
    public let language: String?
    public let facets: [LeafletFacet]?

    // MARK: - Math
    public let tex: String?

    // MARK: - Image
    public let image: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let alt: String?

    // MARK: - Lists
    public let children: [LeafletListItem]?
    public let startIndex: Int?
    public let orderedListChildren: IndirectBox<LeafletBlockContainer>?

    // MARK: - bskyPost
    public let subject: ComAtprotoLexicon.Repository.StrongReference?
    public let clientHost: String?

    // MARK: - standardSitePost
    /// AT-URI of the embedded standard.site document (decoded from the
    /// string form of the block's `subject` key; bskyPost/poll use the
    /// object form, which is stored in `subject`).
    public let standardSitePostSubject: String?
    /// CID of the embedded document's record (for verification).
    public let standardSitePostCID: String?
    /// Display size: "small" or "medium".
    public let size: String?
    /// Whether to show the source publication's theme on the embed.
    public let showPublicationTheme: Bool?

    // MARK: - website (link preview card)
    public let url: String?
    /// The website's og:title or equivalent.
    public let websiteTitle: String?
    /// The website's og:description or equivalent.
    public let websiteDescription: String?

    // MARK: - button
    public let text: String?

    // MARK: - postsList
    /// Display mode: "small" or "medium".
    public let listView: String?
    public let highlightFirstPost: Bool?
    public let filterByTags: [String]?

    // MARK: - signup
    // No additional fields — empty object.

    // MARK: - page (internal page reference)
    /// Zero-based page index within the document.
    public let pageIndex: Int?
    /// AT-URI of the referenced document (when linking across documents).
    public let pageDocument: String?

    // MARK: - iframe
    public let height: Double?
    public let aspectRatio: String?

    // MARK: - Init

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
        orderedListChildren: IndirectBox<LeafletBlockContainer>? = nil,
        subject: ComAtprotoLexicon.Repository.StrongReference? = nil,
        clientHost: String? = nil,
        standardSitePostSubject: String? = nil,
        standardSitePostCID: String? = nil,
        size: String? = nil,
        showPublicationTheme: Bool? = nil,
        url: String? = nil,
        websiteTitle: String? = nil,
        websiteDescription: String? = nil,
        text: String? = nil,
        listView: String? = nil,
        highlightFirstPost: Bool? = nil,
        filterByTags: [String]? = nil,
        pageIndex: Int? = nil,
        pageDocument: String? = nil,
        height: Double? = nil,
        aspectRatio: String? = nil
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
        self.subject = subject
        self.clientHost = clientHost
        self.standardSitePostSubject = standardSitePostSubject
        self.standardSitePostCID = standardSitePostCID
        self.size = size
        self.showPublicationTheme = showPublicationTheme
        self.url = url
        self.websiteTitle = websiteTitle
        self.websiteDescription = websiteDescription
        self.text = text
        self.listView = listView
        self.highlightFirstPost = highlightFirstPost
        self.filterByTags = filterByTags
        self.pageIndex = pageIndex
        self.pageDocument = pageDocument
        self.height = height
        self.aspectRatio = aspectRatio
    }

    // MARK: - Decodable

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)

        type     = try container.decode(String.self, forKey: .type)
        plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        level    = try container.decodeIfPresent(Int.self, forKey: .level)
        language = try container.decodeIfPresent(String.self, forKey: .language)
        tex      = try container.decodeIfPresent(String.self, forKey: .tex)
        image    = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .image)
        alt      = try container.decodeIfPresent(String.self, forKey: .alt)
        facets   = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        children = try container.decodeIfPresent([LeafletListItem].self, forKey: .children)
        startIndex = try container.decodeIfPresent(Int.self, forKey: .startIndex)
        orderedListChildren = try container.decodeIfPresent(IndirectBox<LeafletBlockContainer>.self, forKey: .orderedListChildren)

        // `subject` is polymorphic across block types:
        // - bskyPost / poll: StrongReference object {uri, cid}
        // - standardSitePost: plain String (AT-URI)
        subject     = try container.decodeIfPresent(ComAtprotoLexicon.Repository.StrongReference.self, forKey: .subject)
        if subject == nil {
            standardSitePostSubject = try container.decodeIfPresent(String.self, forKey: .subject)
        } else {
            standardSitePostSubject = nil
        }

        clientHost  = try container.decodeIfPresent(String.self, forKey: .clientHost)
        standardSitePostCID = try container.decodeIfPresent(String.self, forKey: .cid)
        size        = try container.decodeIfPresent(String.self, forKey: .size)
        showPublicationTheme = try container.decodeIfPresent(Bool.self, forKey: .showPublicationTheme)

        url                = try container.decodeIfPresent(String.self, forKey: .url)
        websiteTitle       = try container.decodeIfPresent(String.self, forKey: .title)
        websiteDescription = try container.decodeIfPresent(String.self, forKey: .description)

        text = try container.decodeIfPresent(String.self, forKey: .text)

        listView           = try container.decodeIfPresent(String.self, forKey: .view)
        highlightFirstPost = try container.decodeIfPresent(Bool.self, forKey: .highlightFirstPost)
        filterByTags       = try container.decodeIfPresent([String].self, forKey: .filterByTags)

        pageIndex    = try container.decodeIfPresent(Int.self, forKey: .page)
        pageDocument = try container.decodeIfPresent(String.self, forKey: .document)

        height       = try container.decodeIfPresent(Double.self, forKey: .height)
        aspectRatio  = try container.decodeIfPresent(String.self, forKey: .aspectRatio)
    }

    // MARK: - Encodable

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

        // Polymorphic subject: encode StrongReference if present, else the string form
        if let subject {
            try container.encode(subject, forKey: .subject)
        } else if let standardSitePostSubject {
            try container.encode(standardSitePostSubject, forKey: .subject)
        }

        try container.encodeIfPresent(clientHost, forKey: .clientHost)
        try container.encodeIfPresent(standardSitePostCID, forKey: .cid)
        try container.encodeIfPresent(size, forKey: .size)
        try container.encodeIfPresent(showPublicationTheme, forKey: .showPublicationTheme)
        try container.encodeIfPresent(url, forKey: .url)
        try container.encodeIfPresent(websiteTitle, forKey: .title)
        try container.encodeIfPresent(websiteDescription, forKey: .description)
        try container.encodeIfPresent(text, forKey: .text)
        try container.encodeIfPresent(listView, forKey: .view)
        try container.encodeIfPresent(highlightFirstPost, forKey: .highlightFirstPost)
        try container.encodeIfPresent(filterByTags, forKey: .filterByTags)
        try container.encodeIfPresent(pageIndex, forKey: .page)
        try container.encodeIfPresent(pageDocument, forKey: .document)
        try container.encodeIfPresent(height, forKey: .height)
        try container.encodeIfPresent(aspectRatio, forKey: .aspectRatio)
    }

    // MARK: - Coding Keys

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext, level, language, tex, image, alt, facets
        case children, startIndex, orderedListChildren
        case subject, clientHost
        case cid
        case size, showPublicationTheme
        case url, title, description
        case text
        case view
        case highlightFirstPost, filterByTags
        case page, document
        case height, aspectRatio
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
