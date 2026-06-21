//
//  MarkpubContent.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  The `at.markpub.markdown` record format — markpub stores GFM markdown
//  directly, so this is the simplest of Inkwell's content providers (see
//  `MarkpubProvider` in ContentProvider.swift): close to a 1:1 mapping with
//  the editor's own markdown, nothing lost on round-trip.
//

import Foundation
import ATProtoKit

// MARK: - at.markpub.markdown

public struct MarkpubContent: ATRecordProtocol {
    public static private(set) var type = "at.markpub.markdown"

    public let text: MarkpubText

    public init(text: MarkpubText) {
        self.text = text
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.text = try container.decode(MarkpubText.self, forKey: .text)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encode(text, forKey: .text)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case text
    }
}

nonisolated public struct MarkpubText: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let markdown: String?
    public let textBlob: ComAtprotoLexicon.Repository.UploadBlobOutput?

    public init(type: String = "at.markpub.text", markdown: String? = nil, textBlob: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil) {
        self.type = type
        self.markdown = markdown
        self.textBlob = textBlob
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.type = try container.decode(String.self, forKey: .type)
        self.markdown = try container.decodeIfPresent(String.self, forKey: .markdown)
        self.textBlob = try container.decodeIfPresent(ComAtprotoLexicon.Repository.UploadBlobOutput.self, forKey: .textBlob)
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(type, forKey: .type)
        try container.encodeIfPresent(markdown, forKey: .markdown)
        try container.encodeIfPresent(textBlob, forKey: .textBlob)
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case markdown
        case textBlob
    }
}
