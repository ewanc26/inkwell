//
//  LoginStateManager+Recommends.swift
//  Inkwell
//

import Foundation
import ATProtoKit

extension LoginStateManager {
    // MARK: - Recommends

    /// Creates a `site.standard.graph.recommend` record.
    @discardableResult
    func createRecommend(documentURI: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard parseAtUri(documentURI)?.collection == SiteStandardLexicon.DocumentRecord.type else {
            throw LoginError.invalidURI
        }
        let recommend = SiteStandardLexicon.Graph.RecommendRecord(
            document: documentURI, createdAt: Date()
        )
        return try await createRecord(
            collection: SiteStandardLexicon.Graph.RecommendRecord.type,
            record: UnknownType.record(recommend)
        )
    }

    /// Fetches the **current user's** recommends (local repo only).
    ///
    /// Used to determine whether the signed-in user has already recommended
    /// a given document. For a global recommend count or list across all
    /// repos, use ``fetchAllRecommends(for:)`` instead.
    func fetchRecommends() async throws -> [RecommendEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }
        let records = try await listAllRecords(
            from: did, collection: SiteStandardLexicon.Graph.RecommendRecord.type
        )
        return records.compactMap { record in
            record.value
                .flatMap { $0.getRecord(ofType: SiteStandardLexicon.Graph.RecommendRecord.self) }
                .map {
                    let rkey = parseAtUri(record.uri)?.recordKey ?? ""
                    return RecommendEntry(uri: record.uri, recordKey: rkey, record: $0)
                }
        }
    }

    /// Fetches **all** recommend records referencing a document, across the
    /// entire AT Protocol network, using the Constellation backlink index.
    ///
    /// Each discovered backlink is hydrated from the recommender's PDS.
    /// Results are deduplicated by URI.
    func fetchAllRecommends(for documentURI: String) async -> [RecommendEntry] {
        let backlinks = await ConstellationClient.getRecommendBacklinks(
            documentURI: documentURI
        )

        var seen = Set<String>()
        var recommends: [RecommendEntry] = []

        await withTaskGroup(of: RecommendEntry?.self) { group in
            for backlink in backlinks {
                let uri = backlink.recordURI
                guard seen.insert(uri).inserted else { continue }

                group.addTask { [backlink] in
                    guard let (recordURI, _, value) = try? await self.getRepositoryRecord(
                        from: backlink.did,
                        collection: backlink.collection,
                        recordKey: backlink.rkey
                    ),
                    let record = value?.getRecord(ofType: SiteStandardLexicon.Graph.RecommendRecord.self),
                    record.document == documentURI else {
                        return nil
                    }
                    return RecommendEntry(uri: recordURI, recordKey: backlink.rkey, record: record)
                }
            }
            for await result in group {
                if let entry = result {
                    recommends.append(entry)
                }
            }
        }

        return recommends
    }

    /// Returns the total count of recommends for a document (across all
    /// repos), using Constellation discovery without hydrating records.
    func fetchRecommendCount(for documentURI: String) async -> Int {
        // A single page is sufficient for counting; the first response
        // includes a `total` field. We ask for 1 record to minimise bytes.
        let result = try? await ConstellationClient.getBacklinks(
            subject: documentURI,
            source: "site.standard.graph.recommend:document",
            limit: 1
        )
        // The total count is available from the Constellation API but we
        // didn't model it. Fall back: paginate and count.
        guard let result else { return 0 }
        if result.cursor == nil {
            return result.backlinks.count
        }
        // Multi-page case — paginate fully.
        let all = await ConstellationClient.getRecommendBacklinks(
            documentURI: documentURI
        )
        return all.count
    }

    /// Returns the AT-URIs of Bluesky posts that link to the given document
    /// URL via facets or external embeds, using Constellation.
    ///
    /// This mirrors leaflet.pub's `getConstellationBacklinks()`.
    func fetchDocumentMentionURIs(for documentURL: String) async -> [String] {
        let backlinks = await ConstellationClient.getDocumentMentionBacklinks(
            url: documentURL
        )
        return backlinks.map(\.recordURI)
    }

    /// Deletes a recommend record.
    func deleteRecommend(recordKey: String) async throws {
        try await deleteRecord(
            collection: SiteStandardLexicon.Graph.RecommendRecord.type,
            recordKey: recordKey
        )
    }
}
