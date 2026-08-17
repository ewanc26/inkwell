//
//  OffprintContent.swift
//  Inkwell
//
//  The `app.offprint.content` record format — offprint.app's block-array
//  content shape. Converted to/from the editor's markdown by
//  `OffprintProvider` in ContentProvider.swift; inline formatting uses
//  ``LeafletFacet`` (see RichTextFacets.swift) under offprint's own
//  `app.offprint.richtext.facet#*` `$type` strings.
//
//  Wire models match the authoritative offprint.app lexicons published at
//  at://did:plc:pgjkomf37an4czloay5zeth6/com.atproto.lexicon.schema.
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

// MARK: - OffprintBlock

public struct OffprintBlock: Codable, Equatable, Hashable, Sendable {
    public let type: String

    // Text / heading / callout
    public let plaintext: String?
    public let level: Int?
    public let facets: [LeafletFacet]?
    public let textAlign: String?

    // Code block
    public let code: String?
    public let language: String?
    public let showLineNumbers: Bool?

    // Blockquote
    public let content: [OffprintBlock]?

    // Lists
    public let children: [OffprintListItem]?
    public let start: Int?

    // Image
    public let image: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let alt: String?
    public let width: String?
    public let caption: String?
    public let alignment: String?
    public let aspectRatio: OffprintAspectRatio?
    public let captionFacets: [LeafletFacet]?

    // Image grid / carousel / diff
    public let images: [OffprintGridImage]?
    public let gridRows: Int?

    // Web / embed / button
    public let href: String?
    public let title: String?
    public let preview: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let siteName: String?
    public let description: String?
    public let embedUrl: String?
    public let embedWidth: Int?
    public let embedHeight: Int?

    // Bluesky post
    public let post: OffprintStrongRef?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        facets: [LeafletFacet]? = nil,
        textAlign: String? = nil,
        code: String? = nil,
        language: String? = nil,
        showLineNumbers: Bool? = nil,
        content: [OffprintBlock]? = nil,
        children: [OffprintListItem]? = nil,
        start: Int? = nil,
        image: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        alt: String? = nil,
        width: String? = nil,
        caption: String? = nil,
        alignment: String? = nil,
        aspectRatio: OffprintAspectRatio? = nil,
        captionFacets: [LeafletFacet]? = nil,
        images: [OffprintGridImage]? = nil,
        gridRows: Int? = nil,
        href: String? = nil,
        title: String? = nil,
        preview: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        siteName: String? = nil,
        description: String? = nil,
        embedUrl: String? = nil,
        embedWidth: Int? = nil,
        embedHeight: Int? = nil,
        post: OffprintStrongRef? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.facets = facets
        self.textAlign = textAlign
        self.code = code
        self.language = language
        self.showLineNumbers = showLineNumbers
        self.content = content
        self.children = children
        self.start = start
        self.image = image
        self.alt = alt
        self.width = width
        self.caption = caption
        self.alignment = alignment
        self.aspectRatio = aspectRatio
        self.captionFacets = captionFacets
        self.images = images
        self.gridRows = gridRows
        self.href = href
        self.title = title
        self.preview = preview
        self.siteName = siteName
        self.description = description
        self.embedUrl = embedUrl
        self.embedWidth = embedWidth
        self.embedHeight = embedHeight
        self.post = post
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case plaintext
        case level
        case facets
        case textAlign
        case code
        case language
        case showLineNumbers
        case content
        case children
        case start
        case image
        case alt
        case width
        case caption
        case alignment
        case aspectRatio
        case captionFacets
        case images
        case gridRows
        case href
        case title
        case preview
        case siteName
        case description
        case embedUrl
        case embedWidth
        case embedHeight
        case post
    }
}

// MARK: - OffprintListItem

public struct OffprintListItem: Codable, Equatable, Hashable, Sendable {
    public let content: OffprintBlock?
    public let checked: Bool?
    public let children: [OffprintListItem]?

    public init(
        content: OffprintBlock? = nil,
        checked: Bool? = nil,
        children: [OffprintListItem]? = nil
    ) {
        self.content = content
        self.checked = checked
        self.children = children
    }

    enum CodingKeys: String, CodingKey {
        case content
        case checked
        case children
    }
}

// MARK: - OffprintAspectRatio

public struct OffprintAspectRatio: Codable, Equatable, Hashable, Sendable {
    public let width: Int
    public let height: Int

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
    }
}

// MARK: - OffprintGridImage

public struct OffprintGridImage: Codable, Equatable, Hashable, Sendable {
    public let alt: String?
    public let blob: ComAtprotoLexicon.Repository.UploadBlobOutput?
    public let aspectRatio: OffprintAspectRatio?

    public init(
        alt: String? = nil,
        blob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        aspectRatio: OffprintAspectRatio? = nil
    ) {
        self.alt = alt
        self.blob = blob
        self.aspectRatio = aspectRatio
    }

    enum CodingKeys: String, CodingKey {
        case alt
        case blob
        case aspectRatio
    }
}

// MARK: - OffprintStrongRef

public struct OffprintStrongRef: Codable, Equatable, Hashable, Sendable {
    public let uri: String
    public let cid: String

    public init(uri: String, cid: String) {
        self.uri = uri
        self.cid = cid
    }

    enum CodingKeys: String, CodingKey {
        case uri
        case cid
    }
}
