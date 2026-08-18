//
//  PcktSupportingTypes.swift
//  Inkwell
//
//  Auxiliary payload shapes for pckt block kinds Inkwell doesn't render
//  inline (mentions, galleries, iframes, website cards, embeds, tables).
//

import Foundation

// MARK: - PcktMention

public struct PcktMention: Codable, Equatable, Hashable, Sendable {
    public let did: String
    public let handle: String

    public init(did: String, handle: String) {
        self.did = did
        self.handle = handle
    }
}

// MARK: - PcktGallery

public struct PcktGallery: Codable, Equatable, Hashable, Sendable {
    public let ref: String

    public init(ref: String) {
        self.ref = ref
    }
}

// MARK: - PcktIframe

public struct PcktIframe: Codable, Equatable, Hashable, Sendable {
    public let url: String
    public let height: Int?

    public init(url: String, height: Int? = nil) {
        self.url = url
        self.height = height
    }
}

// MARK: - PcktWebsite

public struct PcktWebsite: Codable, Equatable, Hashable, Sendable {
    public let src: String
    public let title: String?
    public let websiteDescription: String?
    public let previewImage: String?

    public init(src: String, title: String? = nil, websiteDescription: String? = nil, previewImage: String? = nil) {
        self.src = src
        self.title = title
        self.websiteDescription = websiteDescription
        self.previewImage = previewImage
    }
}

// MARK: - PcktBlueskyEmbed

public struct PcktBlueskyEmbed: Codable, Equatable, Hashable, Sendable {
    public let postRef: String

    public init(postRef: String) {
        self.postRef = postRef
    }
}

// MARK: - PcktTableRow

public struct PcktTableRow: Codable, Equatable, Hashable, Sendable {
    public let content: [PcktTableCell]

    public init(content: [PcktTableCell]) {
        self.content = content
    }
}

// MARK: - PcktTableCell / PcktTableHeader

public struct PcktTableCell: Codable, Equatable, Hashable, Sendable {
    public let content: [PcktBlock]
    public let colspan: Int?
    public let rowspan: Int?

    public init(content: [PcktBlock], colspan: Int? = nil, rowspan: Int? = nil) {
        self.content = content
        self.colspan = colspan
        self.rowspan = rowspan
    }
}

public struct PcktTableHeader: Codable, Equatable, Hashable, Sendable {
    public let content: [PcktBlock]
    public let colspan: Int?
    public let rowspan: Int?

    public init(content: [PcktBlock], colspan: Int? = nil, rowspan: Int? = nil) {
        self.content = content
        self.colspan = colspan
        self.rowspan = rowspan
    }
}

// MARK: - PcktNoteEmbed

public struct PcktNoteEmbed: Codable, Equatable, Hashable, Sendable {
    public let noteRef: String

    public init(noteRef: String) {
        self.noteRef = noteRef
    }
}
