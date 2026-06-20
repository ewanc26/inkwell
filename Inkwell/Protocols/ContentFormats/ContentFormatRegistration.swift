//
//  ContentFormatRegistration.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  Registers every platform-specific `content` format Inkwell knows how to
//  read/write with ATProtoKit's `ATRecordTypeRegistry`, so
//  `SiteStandardLexicon.DocumentRecord.content` (an open-union
//  `UnknownType`, deliberately left undefined by standard.site itself) can
//  resolve to one of these concrete structs by `$type` rather than falling
//  back to raw JSON.
//
//  Deliberately kept separate from `SiteStandardRegistration.swift`: these
//  aren't `site.standard.*` lexicons, they're each publishing platform's
//  own content schema (Leaflet, Markpub, Pckt, Offprint) — see the note in
//  `SiteStandardLexicon.swift`. Both are called from `InkwellApp`'s
//  `.task`, before anything attempts to decode a document.

import Foundation
import ATProtoKit

/// Registration for the `content`-format record types ContentProvider.swift's
/// providers convert to/from markdown (see `ProviderRegistry`).
enum ContentFormatRegistration {

    /// Call this once, early in the app's lifetime — see `InkwellApp.swift`'s
    /// `.task`, alongside `SiteStandardLexicon.registerRecordTypes()`. Safe
    /// to call more than once; `ATRecordTypeRegistry` skips any type that's
    /// already registered.
    static func registerRecordTypes() async {
        await ATRecordTypeRegistry.shared.register(types: [
            LeafletContent.self,
            MarkpubContent.self,
            PcktContent.self,
            OffprintContent.self
        ])
    }
}
