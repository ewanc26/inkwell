//
//  SiteStandardRegistration.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon {

    /// Registers every `site.standard.*` record type with ATProtoKit's
    /// `ATRecordTypeRegistry`, so `UnknownType` elsewhere in the SDK — including these
    /// lexicons' own open-union fields, like ``DocumentRecord/content`` — can resolve a
    /// `$type` it encounters back to one of these concrete structs instead of falling back to
    /// raw, untyped JSON.
    ///
    /// Call this once, early in the app's lifetime — see `InkwellApp.swift`'s `.task`, where
    /// it runs alongside `LoginStateManager.restoreSessionIfPossible()`. Safe to call more
    /// than once; `ATRecordTypeRegistry` skips any type that's already registered.
    public static func registerRecordTypes() async {
        await ATRecordTypeRegistry.shared.register(types: [
            SiteStandardLexicon.PublicationRecord.self,
            SiteStandardLexicon.DocumentRecord.self,
            SiteStandardLexicon.Graph.SubscriptionRecord.self,
            SiteStandardLexicon.Graph.RecommendRecord.self,
            PubLeafletComment.self,
        ])
    }
}
