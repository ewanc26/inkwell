//
//  StandardSiteTypes.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Wrapper types that carry the AT-URI and author DID alongside decoded
//  standard.site records. The lexicon structs (PublicationRecord,
//  DocumentRecord, etc.) only model the record *value* — they don't know
//  their own AT-URI or which repo they live in. These wrappers fill that
//  gap so the UI and subscription/notification code can reference records
//  by URI and fetch cross-repo content.
//

import Foundation

// MARK: - AT-URI Parser

/// Parses AT-URIs (`at://did:plc:abc123/collection/rkey`) into their
/// components. Used for extracting the author DID and record key from
/// subscription and publication AT-URIs.
struct ATURI: Equatable, Hashable {
    let did: String
    let collection: String
    let recordKey: String

    /// The full AT-URI string.
    var uri: String { "at://\(did)/\(collection)/\(recordKey)" }

    /// Parses an AT-URI string. Returns `nil` if the format is invalid.
    static func parse(_ uri: String) -> ATURI? {
        guard let result = parseAtUri(uri) else { return nil }
        return ATURI(did: result.did, collection: result.collection, recordKey: result.recordKey)
    }
}

// MARK: - Entry Wrappers

/// A publication record enriched with its AT-URI and author DID.
struct PublicationEntry: Identifiable, Equatable, Hashable {
    /// The AT-URI of the publication record.
    let uri: String
    /// The DID of the repo that contains this publication.
    let authorDID: String
    /// The decoded publication record.
    let record: SiteStandardLexicon.PublicationRecord

    var id: String { uri }
}

extension PublicationEntry {
    /// Matches both the preferred publication AT-URI and the URL form used by
    /// older and standalone Standard.site producers.
    nonisolated func contains(_ document: SiteStandardLexicon.DocumentRecord) -> Bool {
        document.site == uri || normalizedSite(document.site) == normalizedSite(record.url)
    }
}

/// A document record enriched with its AT-URI and author DID.
struct DocumentEntry: Identifiable, Equatable, Hashable {
    /// The AT-URI of the document record.
    let uri: String
    /// The DID of the repo that contains this document.
    let authorDID: String
    /// The decoded document record.
    let record: SiteStandardLexicon.DocumentRecord

    var id: String { uri }
}

extension SiteStandardLexicon.DocumentRecord {
    /// Builds the canonical web URL described by Standard.site's `site` + `path`
    /// rules. A publication is required when `site` is an AT-URI.
    func canonicalURL(publication: SiteStandardLexicon.PublicationRecord? = nil) -> URL? {
        guard let urlString = canonicalUrl(
            site: site,
            path: path,
            publicationURL: publication?.url
        ) else { return nil }
        return URL(string: urlString)
    }
}

/// A subscription record enriched with its AT-URI and record key.
struct SubscriptionEntry: Identifiable, Equatable, Hashable {
    /// The AT-URI of the subscription record itself.
    let uri: String
    /// The record key (needed to delete the subscription).
    let recordKey: String
    /// The decoded subscription record (contains the publication AT-URI).
    let record: SiteStandardLexicon.Graph.SubscriptionRecord

    var id: String { uri }

    /// The parsed AT-URI of the publication being subscribed to.
    var publicationURI: ATURI? { ATURI.parse(record.publication) }
}

/// A comment record enriched with its AT-URI and record key.
struct CommentEntry: Identifiable, Equatable, Hashable {
    let uri: String
    let recordKey: String
    let record: PubLeafletComment

    var id: String { uri }
}

/// A recommend record enriched with its AT-URI and record key.
struct RecommendEntry: Identifiable, Equatable, Hashable {
    /// The AT-URI of the recommend record itself.
    let uri: String
    /// The record key.
    let recordKey: String
    /// The decoded recommend record (contains the document AT-URI).
    let record: SiteStandardLexicon.Graph.RecommendRecord

    var id: String { uri }

    /// The parsed AT-URI of the document being recommended.
    var documentURI: ATURI? { ATURI.parse(record.document) }
}
