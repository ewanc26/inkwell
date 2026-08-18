//
//  PcktBlock.swift
//  Inkwell
//

import Foundation
import ATProtoKit

// MARK: - PcktBlock

public struct PcktBlock: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let plaintext: String?
    public let level: Int?
    public let language: String?
    public let facets: [LeafletFacet]?
    public let textAlign: String?

    /// Nested blocks, for a blockquote's inner paragraphs.
    public let content: [PcktBlock]?

    /// List items, for `bulletList`/`orderedList`/`taskList`. Pckt's real
    /// lexicon keys this under the *same* `content` field as the blockquote
    /// case above (not `children`) — see standard.horse's `pckt.ts`
    /// `listToMdast`/`listBlock`. Kept as a separate Swift property since the
    /// element shape differs (list items vs. nested blocks); `init(from:)`/
    /// `encode(to:)` below route both through the one JSON key by `$type`.
    public let listContent: [PcktListItem]?

    /// Table rows, for `table`.
    public let tableRows: [PcktTableRow]?

    /// Start index for `orderedList`.
    public let start: Int?
    public let attrs: PcktBlockAttrs?

    // --- Unsupported block payloads ---

    /// `blog.pckt.block.mention` — `did`, `handle`
    public let did: String?
    public let handle: String?

    /// `blog.pckt.block.gallery` — `ref` (at-uri)
    public let ref: String?

    /// `blog.pckt.block.iframe` — `url`, `height`
    public let url: String?
    public let height: Int?

    /// `blog.pckt.block.website` — `title`, `description`, `previewImage`
    public let websiteTitle: String?
    public let websiteDescription: String?
    public let previewImage: String?

    /// `blog.pckt.block.blueskyEmbed` — `postRef` (strongRef uri)
    public let postRef: String?

    /// `blog.pckt.block.noteEmbed` — `noteRef` (strongRef uri)
    public let noteRef: String?

    public init(
        type: String,
        plaintext: String? = nil,
        level: Int? = nil,
        language: String? = nil,
        facets: [LeafletFacet]? = nil,
        textAlign: String? = nil,
        content: [PcktBlock]? = nil,
        listContent: [PcktListItem]? = nil,
        tableRows: [PcktTableRow]? = nil,
        start: Int? = nil,
        attrs: PcktBlockAttrs? = nil,
        did: String? = nil,
        handle: String? = nil,
        ref: String? = nil,
        url: String? = nil,
        height: Int? = nil,
        websiteTitle: String? = nil,
        websiteDescription: String? = nil,
        previewImage: String? = nil,
        postRef: String? = nil,
        noteRef: String? = nil
    ) {
        self.type = type
        self.plaintext = plaintext
        self.level = level
        self.language = language
        self.facets = facets
        self.textAlign = textAlign
        self.content = content
        self.listContent = listContent
        self.tableRows = tableRows
        self.start = start
        self.attrs = attrs
        self.did = did
        self.handle = handle
        self.ref = ref
        self.url = url
        self.height = height
        self.websiteTitle = websiteTitle
        self.websiteDescription = websiteDescription
        self.previewImage = previewImage
        self.postRef = postRef
        self.noteRef = noteRef
    }

    private static let listBlockSuffixes = ["bulletList", "orderedList", "taskList"]

    private static func isListBlock(_ type: String) -> Bool {
        listBlockSuffixes.contains { type.hasSuffix($0) }
    }

    private static let tableBlockSuffixes = ["table"]

    private static func isTableBlock(_ type: String) -> Bool {
        tableBlockSuffixes.contains { type.hasSuffix($0) }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.plaintext = try container.decodeIfPresent(String.self, forKey: .plaintext)
        self.level = try container.decodeIfPresent(Int.self, forKey: .level)
        self.language = try container.decodeIfPresent(String.self, forKey: .language)
        self.facets = try container.decodeIfPresent([LeafletFacet].self, forKey: .facets)
        self.textAlign = try container.decodeIfPresent(String.self, forKey: .textAlign)
        self.start = try container.decodeIfPresent(Int.self, forKey: .start)
        self.attrs = try container.decodeIfPresent(PcktBlockAttrs.self, forKey: .attrs)

        self.did = try container.decodeIfPresent(String.self, forKey: .did)
        self.handle = try container.decodeIfPresent(String.self, forKey: .handle)
        self.ref = try container.decodeIfPresent(String.self, forKey: .ref)
        self.url = try container.decodeIfPresent(String.self, forKey: .url)
        self.height = try container.decodeIfPresent(Int.self, forKey: .height)
        self.websiteTitle = try container.decodeIfPresent(String.self, forKey: .websiteTitle)
        self.websiteDescription = try container.decodeIfPresent(String.self, forKey: .websiteDescription)
        self.previewImage = try container.decodeIfPresent(String.self, forKey: .previewImage)
        self.postRef = try container.decodeIfPresent(String.self, forKey: .postRef)
        self.noteRef = try container.decodeIfPresent(String.self, forKey: .noteRef)

        // `content` is polymorphic in pckt's real lexicon: nested blocks for
        // a blockquote, list items for bulletList/orderedList/taskList,
        // table rows for table.
        if Self.isTableBlock(self.type) {
            self.tableRows = try container.decodeIfPresent([PcktTableRow].self, forKey: .content)
            self.content = nil
            self.listContent = nil
        } else if Self.isListBlock(self.type) {
            self.listContent = try container.decodeIfPresent([PcktListItem].self, forKey: .content)
            self.content = nil
            self.tableRows = nil
        } else {
            self.content = try container.decodeIfPresent([PcktBlock].self, forKey: .content)
            self.listContent = nil
            self.tableRows = nil
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(plaintext, forKey: .plaintext)
        try container.encodeIfPresent(level, forKey: .level)
        try container.encodeIfPresent(language, forKey: .language)
        try container.encodeIfPresent(facets, forKey: .facets)
        try container.encodeIfPresent(textAlign, forKey: .textAlign)
        try container.encodeIfPresent(start, forKey: .start)
        try container.encodeIfPresent(attrs, forKey: .attrs)

        try container.encodeIfPresent(did, forKey: .did)
        try container.encodeIfPresent(handle, forKey: .handle)
        try container.encodeIfPresent(ref, forKey: .ref)
        try container.encodeIfPresent(url, forKey: .url)
        try container.encodeIfPresent(height, forKey: .height)
        try container.encodeIfPresent(websiteTitle, forKey: .websiteTitle)
        try container.encodeIfPresent(websiteDescription, forKey: .websiteDescription)
        try container.encodeIfPresent(previewImage, forKey: .previewImage)
        try container.encodeIfPresent(postRef, forKey: .postRef)
        try container.encodeIfPresent(noteRef, forKey: .noteRef)

        if let tableRows {
            try container.encode(tableRows, forKey: .content)
        } else if let listContent {
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
        case textAlign
        case content
        case listContent
        case tableRows
        case start
        case attrs
        case did
        case handle
        case ref
        case url
        case height
        case websiteTitle
        case websiteDescription
        case previewImage
        case postRef
        case noteRef
    }
}

// MARK: - PcktBlockAttrs

public struct PcktBlockAttrs: Codable, Equatable, Hashable, Sendable {
    public let blob: ComAtprotoLexicon.Repository.UploadBlobOutput?
    /// pckt's actual attribute name for an image's source (CID-as-blob or
    /// an external URL) is `src`, not `url` — see standard.horse's
    /// `pckt.ts` `imageBlock`/`blockToMdast`.
    public let src: String?
    public let alt: String?
    public let align: String?
    public let title: String?
    public let aspectRatio: PcktAspectRatio?

    public init(
        blob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil,
        src: String? = nil,
        alt: String? = nil,
        align: String? = nil,
        title: String? = nil,
        aspectRatio: PcktAspectRatio? = nil
    ) {
        self.blob = blob
        self.src = src
        self.alt = alt
        self.align = align
        self.title = title
        self.aspectRatio = aspectRatio
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.src = try container.decodeIfPresent(String.self, forKey: .src)
        self.alt = try container.decodeIfPresent(String.self, forKey: .alt)
        self.align = try container.decodeIfPresent(String.self, forKey: .align)
        self.title = try container.decodeIfPresent(String.self, forKey: .title)
        self.aspectRatio = try container.decodeIfPresent(PcktAspectRatio.self, forKey: .aspectRatio)

        self.blob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .blob)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(src, forKey: .src)
        try container.encodeIfPresent(alt, forKey: .alt)
        try container.encodeIfPresent(align, forKey: .align)
        try container.encodeIfPresent(title, forKey: .title)
        try container.encodeIfPresent(aspectRatio, forKey: .aspectRatio)
        try container.encodeIfPresent(blob, forKey: .blob)
    }

    enum CodingKeys: String, CodingKey {
        case blob
        case src
        case alt
        case align
        case title
        case aspectRatio
    }
}
