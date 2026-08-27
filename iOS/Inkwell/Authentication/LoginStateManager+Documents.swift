//
//  LoginStateManager+Documents.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

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

    /// Creates and publishes a new document record.
    @discardableResult
    func createDocument(
        title: String,
        description: String?,
        path: String?,
        site: String,
        markdown: String,
        provider: ContentProvider,
        previousContent: UnknownType? = nil
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard currentDID != nil else { throw LoginError.notAuthenticated }

        let ctx = WriteContext(previousContent: previousContent)
        guard let contentRecord = provider.fromMarkdown(markdown, ctx: ctx) else {
            throw LoginError.contentConversionFailed
        }

        guard (parseAtUri(site)?.collection == SiteStandardLexicon.PublicationRecord.type) ||
                (URL(string: site)?.scheme?.lowercased() == "https") else {
            throw LoginError.invalidURI
        }

        let normalizedPath = path.flatMap { value -> String? in
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !trimmed.isEmpty else { return nil }
            return trimmed.hasPrefix("/") ? trimmed : "/\(trimmed)"
        }
        let plainText = (try? AttributedString(markdown: markdown))
            .map { String($0.characters) }
            .flatMap { $0.isEmpty ? nil : $0 }

        var normalizedSite = site
        while normalizedSite.hasSuffix("/") {
            normalizedSite.removeLast()
        }

        let document = SiteStandardLexicon.DocumentRecord(
            site: normalizedSite,
            title: title,
            publishedAt: Date(),
            path: normalizedPath,
            description: description,
            coverImage: nil,
            content: contentRecord,
            textContent: plainText
        )

        return try await createRecord(
            collection: SiteStandardLexicon.DocumentRecord.type,
            record: UnknownType.record(document)
        )
    }
}
