//
//  LoginStateManager+Subscriptions.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

extension LoginStateManager {
    // MARK: - Subscriptions

    /// Creates a `site.standard.graph.subscription` record.
    @discardableResult
    func createSubscription(publicationURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard parseAtUri(publicationURI)?.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw LoginError.invalidURI
        }
        _cachedSubscriptions = nil  // invalidate cache
        let subscription = SiteStandardLexicon.Graph.SubscriptionRecord(
            publication: publicationURI, createdAt: Date()
        )
        return try await createRecord(
            collection: SiteStandardLexicon.Graph.SubscriptionRecord.type,
            record: UnknownType.record(subscription)
        )
    }

    /// Fetches the user's subscriptions. Results are cached in-memory for the
    /// lifetime of the session so concurrent callers (e.g. BrowseDocumentsView
    /// and NotificationManager) don't race DPoP nonces against each other.
    func fetchSubscriptions() async throws -> [SubscriptionEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }

        // Return a cached snapshot when available — avoids redundant
        // authenticated listRecords calls that would collide on DPoP nonces.
        if let cached = _cachedSubscriptions { return cached }

        let records = try await listAllRecords(
            from: did, collection: SiteStandardLexicon.Graph.SubscriptionRecord.type
        )
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.Graph.SubscriptionRecord.self) }
                .map {
                    let rkey = parseAtUri(record.uri)?.recordKey ?? ""
                    return SubscriptionEntry(uri: record.uri, recordKey: rkey, record: $0)
                }
        }
        _cachedSubscriptions = decoded
        logger.info("[fetchSubscriptions] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchSubscriptions] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Deletes a subscription record.
    func deleteSubscription(recordKey: String) async throws {
        _cachedSubscriptions = nil  // invalidate cache
        try await deleteRecord(
            collection: SiteStandardLexicon.Graph.SubscriptionRecord.type,
            recordKey: recordKey
        )
    }
}
