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
//  Wire models match the authoritative pckt.blog lexicons published at
//  at://did:plc:revjuqmkvrw6fnkxppqtszpv/com.atproto.lexicon.schema.
//
//  Split across files in this directory: PcktBlock.swift (block + attrs),
//  PcktListItem.swift, and PcktSupportingTypes.swift (auxiliary payload
//  shapes for unsupported block kinds).
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
