//
//  LoginStateManager+Documents.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

/// A completed document write: the new strong reference plus the record exactly
/// as it was submitted. Callers that stay on the editing screen need both — the
/// CID is the next write's revision, and the merged record is the next write's
/// unknown-field preservation baseline.
struct DocumentWriteResult {
    let reference: ComAtprotoLexicon.Repository.StrongReference
    let record: UnknownType
}

extension LoginStateManager {
    // MARK: - Documents

    /// Fetches all of the user's document records.
    func fetchDocuments() async throws -> [SiteStandardLexicon.DocumentRecord] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(from: did, collection: SiteStandardLexicon.DocumentRecord.type)
        let decoded = records.compactMap { $0.value?.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
        logger.info("[fetchDocuments] \(records.count) raw → \(decoded.count) decoded")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchDocuments] 0/\(records.count) records decoded — type registration issue?")
        }
        return decoded
    }

    /// Fetches documents from any user's repository.
    ///
    /// Always uses unauthenticated requests to avoid DPoP errors when
    /// fetching public records through Discover.
    func fetchDocuments(fromDID did: String) async throws -> [DocumentEntry] {
        let records = try await listAllRecords(
            from: did,
            collection: SiteStandardLexicon.DocumentRecord.type,
            forceUnauthenticated: true
        )
        let decoded = records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) }
                .map { DocumentEntry(uri: record.uri, cid: record.cid, authorDID: did, record: $0) }
        }
        logger.info("[fetchDocumentsEntry] \(records.count) raw → \(decoded.count) decoded DocumentEntry")
        if decoded.isEmpty && !records.isEmpty {
            logger.warning("[fetchDocumentsEntry] 0/\(records.count) records decoded for \(did)")
        }
        return decoded
    }

    /// Fetches documents from the current user's repository with URIs.
    func fetchDocumentsWithURIs() async throws -> [DocumentEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        return try await fetchDocuments(fromDID: did)
    }

    /// Fetches one document by AT-URI.
    ///
    /// Always uses unauthenticated requests to avoid DPoP errors when
    /// fetching public records through Discover.
    func fetchDocument(uri: String) async throws -> DocumentEntry {
        guard let parsed = parseAtUri(uri),
              parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw LoginError.invalidURI
        }
        let (recordURI, cid, value) = try await getRepositoryRecord(
            from: parsed.did, collection: parsed.collection, recordKey: parsed.recordKey,
            forceUnauthenticated: true
        )
        guard let document = value?.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
            throw LoginError.unexpectedRecordType
        }
        return DocumentEntry(uri: recordURI, cid: cid, authorDID: parsed.did, record: document)
    }

    /// Creates and publishes a document record composed by the Writer.
    ///
    /// Composition, content conversion, and blob spillover happen in
    /// `DocumentRecordComposer`/`DocumentContentSpill`; this is the
    /// protocol-level write, including the final size preflight.
    @discardableResult
    func createDocument(
        _ document: SiteStandardLexicon.DocumentRecord
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard currentDID != nil else { throw LoginError.notAuthenticated }
        try validateDocumentSite(document.site)
        try ensureDocumentRecordFits(UnknownType.record(document))

        return try await createRecord(
            collection: SiteStandardLexicon.DocumentRecord.type,
            record: UnknownType.record(document)
        )
    }

    /// Updates an existing document record, preserving fields Inkwell doesn't
    /// model so an edit never strips another client's data.
    @discardableResult
    func updateDocument(
        _ document: SiteStandardLexicon.DocumentRecord,
        recordKey: String,
        recordCID: String,
        existingRawRecord: UnknownType?
    ) async throws -> DocumentWriteResult {
        guard currentDID != nil else { throw LoginError.notAuthenticated }
        try validateDocumentSite(document.site)

        let merged = preservingUnknownFields(
            from: existingRawRecord,
            with: UnknownType.record(document)
        )
        try ensureDocumentRecordFits(merged)

        let reference = try await updateRecord(
            collection: SiteStandardLexicon.DocumentRecord.type,
            recordKey: recordKey,
            record: merged,
            recordCID: recordCID
        )
        return DocumentWriteResult(reference: reference, record: merged)
    }

    private func validateDocumentSite(_ site: String) throws {
        guard (parseAtUri(site)?.collection == SiteStandardLexicon.PublicationRecord.type) ||
                (URL(string: site)?.scheme?.lowercased() == "https") else {
            throw LoginError.invalidURI
        }
    }
}
