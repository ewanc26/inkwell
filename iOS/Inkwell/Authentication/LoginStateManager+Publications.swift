//
//  LoginStateManager+Publications.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

extension LoginStateManager {
    // MARK: - Publications

    /// Fetches all of the user's publication records.
    func fetchPublications() async throws -> [SiteStandardLexicon.PublicationRecord] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.PublicationRecord.type)
        let decoded = records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
        logger.info("[fetchPublications] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchPublications] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Fetches publications from any user's repository.
    ///
    /// Always uses unauthenticated requests to avoid DPoP errors when
    /// fetching public records through Discover.
    func fetchPublications(fromDID did: String) async throws -> [PublicationEntry] {
        let records = try await listAllRecords(
            from: did,
            collection: SiteStandardLexicon.PublicationRecord.type,
            forceUnauthenticated: true
        )
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) }
                .map { PublicationEntry(uri: record.uri, authorDID: did, record: $0) }
        }
        logger.info("[fetchPublicationsEntry] \(records.count) raw → \(decoded.count) decoded PublicationEntry")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchPublicationsEntry] 0/\(records.count) records decoded for \(did)")
        }
        return decoded
    }

    /// Fetches publications from the current user's repository with URIs.
    func fetchPublicationsWithURIs() async throws -> [PublicationEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        return try await fetchPublications(fromDID: did)
    }

    /// Fetches one publication by AT-URI.
    ///
    /// Always uses unauthenticated requests to avoid DPoP errors when
    /// fetching public records through Discover.
    func fetchPublication(uri: String) async throws -> PublicationEntry {
        guard let parsed = parseAtUri(uri),
              parsed.collection == SiteStandardLexicon.PublicationRecord.type else {
            throw LoginError.invalidURI
        }
        let (recordURI, _, value) = try await getRepositoryRecord(
            from: parsed.did, collection: parsed.collection, recordKey: parsed.recordKey,
            forceUnauthenticated: true
        )
        guard let publication = value?.getRecord(ofType: SiteStandardLexicon.PublicationRecord.self) else {
            throw LoginError.unexpectedRecordType
        }
        return PublicationEntry(uri: recordURI, authorDID: parsed.did, record: publication)
    }

    /// Creates a publication record.
    @discardableResult
    func createPublication(
        url: String,
        name: String,
        description: String?
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        let record = UnknownType.record(
            SiteStandardLexicon.PublicationRecord(url: url, name: name, description: description)
        )
        return try await createRecord(
            collection: SiteStandardLexicon.PublicationRecord.type,
            record: record
        )
    }
}
