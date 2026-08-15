//
//  RichTextFacets.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  Shared inline-formatting primitives used by every standard.site `content`
//  provider Inkwell understands (Leaflet, Pckt, Offprint — see
//  ContentProvider.swift). None of this is part of standard.site itself:
//  it's the byte-range "facet" shape Bluesky popularised for
//  `app.bsky.richtext.facet`, which Leaflet/Pckt/Offprint each re-declare
//  under their own NSID rather than sharing Bluesky's. The wire shape is
//  identical across all three, so one Swift type does for all of them —
//  only the `$type` strings inside `features` differ (see
//  `ContentProvider.swift`'s `FacetSchema`).
//

import Foundation
import ATProtoKit

/// A single facet: a byte range plus the formatting features applied to it.
public struct LeafletFacet: Codable, Equatable, Hashable, Sendable {
    public let type: String?
    public let index: LeafletByteSlice
    public let features: [LeafletFacetFeature]

    public init(type: String? = nil, index: LeafletByteSlice, features: [LeafletFacetFeature]) {
        self.type = type
        self.index = index
        self.features = features
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case index
        case features
    }
}

/// A UTF-8 byte range within a block's plaintext, used by ``LeafletFacet``.
public struct LeafletByteSlice: Codable, Equatable, Hashable, Sendable {
    public let byteStart: Int
    public let byteEnd: Int

    public init(byteStart: Int, byteEnd: Int) {
        self.byteStart = byteStart
        self.byteEnd = byteEnd
    }
}

/// A single formatting feature within a facet (bold, italic, code, strike, link).
///
/// `uri`/`tag`/`did` are mutually exclusive depending on `type` — only `link`
/// features use `uri`, matching the same convention as Bluesky's own
/// richtext facets.
public struct LeafletFacetFeature: Codable, Equatable, Hashable, Sendable {
    public let type: String
    public let uri: String?
    public let tag: String?
    public let did: String?

    public init(type: String, uri: String? = nil, tag: String? = nil, did: String? = nil) {
        self.type = type
        self.uri = uri
        self.tag = tag
        self.did = did
    }

    enum CodingKeys: String, CodingKey {
        case type = "$type"
        case uri
        case tag
        case did
    }
}

/// A `final class` box used to break the value-type recursion that a
/// directly-recursive `struct` can't express in Swift (`LeafletBlock` and
/// `LeafletBlockContainer` reference each other through ordered-list
/// children). Only Leaflet's block tree currently needs this, but the box
/// itself is generic so any future format with the same circular-list shape
/// can reuse it instead of redefining its own indirection wrapper.
public final class IndirectBox<T: Codable>: Codable, Equatable, Hashable, Sendable where T: Equatable & Hashable {
    public let value: T

    public init(_ value: T) {
        self.value = value
    }

    public init(from decoder: Decoder) throws {
        self.value = try T(from: decoder)
    }

    public func encode(to encoder: Encoder) throws {
        try value.encode(to: encoder)
    }

    public static func == (lhs: IndirectBox<T>, rhs: IndirectBox<T>) -> Bool {
        lhs.value == rhs.value
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(value)
    }
}
