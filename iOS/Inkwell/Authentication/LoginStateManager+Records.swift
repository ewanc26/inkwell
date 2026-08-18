//
//  LoginStateManager+Records.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

extension LoginStateManager {
    // MARK: - Record CRUD

    /// Creates an AT Protocol record in the user's repository.
    @discardableResult
    func createRecord(
        collection: String,
        record: UnknownType,
        shouldValidate: Bool = false
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }

        struct CreateRecordBody: Encodable {
            let repo: String
            let collection: String
            let record: UnknownType
            let validate: Bool

            enum CodingKeys: String, CodingKey {
                case repo, collection, record, validate
            }
        }

        let body = CreateRecordBody(
            repo: did,
            collection: collection,
            record: record,
            validate: shouldValidate
        )
        let bodyData = try JSONEncoder().encode(body)

        let data = try await authenticatedData(
            path: sharedXrpcRepoCreateRecord(),
            method: "POST",
            body: bodyData
        )

        return try JSONDecoder().decode(ComAtprotoLexicon.Repository.StrongReference.self, from: data)
    }

    /// Deletes an AT Protocol record from the user's repository.
    func deleteRecord(collection: String, recordKey: String) async throws {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }

        struct DeleteRecordBody: Encodable {
            let repo: String
            let collection: String
            let rkey: String

            enum CodingKeys: String, CodingKey {
                case repo, collection, rkey
            }
        }

        let bodyData = try JSONEncoder().encode(
            DeleteRecordBody(repo: did, collection: collection, rkey: recordKey)
        )

        _ = try await authenticatedData(
            path: sharedXrpcRepoDeleteRecord(),
            method: "POST",
            body: bodyData
        )
    }

    /// Updates an existing AT Protocol record in the user's repository.
    @discardableResult
    func updateRecord(
        collection: String,
        recordKey: String,
        record: UnknownType,
        revision: String
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }

        struct PutRecordBody: Encodable {
            let repo: String
            let collection: String
            let rkey: String
            let record: UnknownType
            let validate: Bool
            let swapCommit: String

            enum CodingKeys: String, CodingKey {
                case repo, collection, rkey, record, validate, swapCommit
            }
        }

        let body = PutRecordBody(
            repo: did,
            collection: collection,
            rkey: recordKey,
            record: record,
            validate: true,
            swapCommit: revision
        )
        let bodyData = try JSONEncoder().encode(body)

        let data = try await authenticatedData(
            path: sharedXrpcRepoPutRecord(),
            method: "POST",
            body: bodyData
        )

        return try JSONDecoder().decode(ComAtprotoLexicon.Repository.StrongReference.self, from: data)
    }

    /// Fetches and decodes a single record from a repository.
    func getRepositoryRecord(
        from did: String,
        collection: String,
        recordKey: String
    ) async throws -> (uri: String, cid: String?, value: UnknownType?) {
        let pdsURL = try await repositoryPDSURL(for: did)

        let queryItems = [
            URLQueryItem(name: "repo", value: did),
            URLQueryItem(name: "collection", value: collection),
            URLQueryItem(name: "rkey", value: recordKey),
        ]

        let data: Data
        if did == currentDID {
            data = try await authenticatedData(
                path: sharedXrpcRepoGetRecord(),
                queryItems: queryItems
            )
        } else {
            data = try await unauthenticatedData(
                pdsURL: pdsURL,
                path: sharedXrpcRepoGetRecord(),
                queryItems: queryItems
            )
        }

        struct GetRecordOutput: Decodable {
            let uri: String
            let cid: String?
            let value: UnknownType?
        }
        let output = try JSONDecoder().decode(GetRecordOutput.self, from: data)
        return (output.uri, output.cid, output.value)
    }

    // MARK: - List Records (cross-repo)

    /// Lists all records of a given collection from a repository,
    /// following pagination cursors up to `maximumCount`.
    func listAllRecords(
        from did: String,
        collection: String,
        maximumCount: Int = sharedMaxRecordsPerList
    ) async throws -> [RepositoryRecord] {
        let pdsURL = try await repositoryPDSURL(for: did)

        var allRecords: [RepositoryRecord] = []
        var cursor: String?

        repeat {
            var queryItems = [
                URLQueryItem(name: "repo", value: did),
                URLQueryItem(name: "collection", value: collection),
                URLQueryItem(name: "limit", value: String(min(100, maximumCount - allRecords.count))),
            ]
            if let cursor {
                queryItems.append(URLQueryItem(name: "cursor", value: cursor))
            }

            // Try unauthenticated first — most PDS servers allow public
            // listRecords for standard.site collections. If the PDS
            // requires authentication (401/403), retry with auth.
            // This avoids DPoP nonce exhaustion when multiple
            // collections are listed in sequence for the same DID.
            let data: Data
            if did == currentDID {
                do {
                    data = try await unauthenticatedData(
                        pdsURL: pdsURL,
                        path: sharedXrpcRepoListRecords(),
                        queryItems: queryItems
                    )
                } catch LoginError.httpError(let status) where status == 401 || status == 403 {
                    data = try await authenticatedData(
                        path: sharedXrpcRepoListRecords(),
                        queryItems: queryItems
                    )
                }
            } else {
                data = try await unauthenticatedData(
                    pdsURL: pdsURL,
                    path: sharedXrpcRepoListRecords(),
                    queryItems: queryItems
                )
            }

            let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
            logger.info("[listAllRecords] \(collection): raw JSON returned \(page.records.count) records (cursor: \(page.cursor ?? "nil"))")
            let withValues = page.records.filter { $0.value != nil }
            logger.info("[listAllRecords] \(collection): \(withValues.count)/\(page.records.count) records have non-nil value")
            allRecords.append(contentsOf: page.records)
            cursor = page.cursor
        } while cursor != nil && allRecords.count < maximumCount

        return Array(allRecords.prefix(maximumCount))
    }

    /// Fetches a single page of records from a repository.
    ///
    /// Unlike ``listAllRecords(from:collection:maximumCount:)``, this makes
    /// exactly one HTTP request — no pagination loop. The caller is responsible
    /// for advancing the cursor to fetch subsequent pages.
    ///
    /// - Parameters:
    ///   - did: The DID whose repository to query.
    ///   - collection: The NSID of the collection to list.
    ///   - limit: The number of records per page (1–100, default 25).
    ///   - cursor: An opaque cursor from a previous page, or `nil` for the first page.
    /// - Returns: A tuple of the decoded records and an optional cursor for the next page.
    func listRecordsPage(
        from did: String,
        collection: String,
        limit: Int = 25,
        cursor: String? = nil
    ) async throws -> (records: [RepositoryRecord], cursor: String?) {
        let pdsURL = try await repositoryPDSURL(for: did)

        var queryItems = [
            URLQueryItem(name: "repo", value: did),
            URLQueryItem(name: "collection", value: collection),
            URLQueryItem(name: "limit", value: String(min(limit, 100))),
        ]
        if let cursor {
            queryItems.append(URLQueryItem(name: "cursor", value: cursor))
        }

        let data: Data
        if did == currentDID {
            do {
                data = try await unauthenticatedData(
                    pdsURL: pdsURL,
                    path: sharedXrpcRepoListRecords(),
                    queryItems: queryItems
                )
            } catch LoginError.httpError(let status) where status == 401 || status == 403 {
                data = try await authenticatedData(
                    path: sharedXrpcRepoListRecords(),
                    queryItems: queryItems
                )
            }
        } else {
            data = try await unauthenticatedData(
                pdsURL: pdsURL,
                path: sharedXrpcRepoListRecords(),
                queryItems: queryItems
            )
        }

        let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)
        logger.info("[listRecordsPage] \(collection): \(page.records.count) records (cursor: \(page.cursor ?? "nil"))")
        return (page.records, page.cursor)
    }
}
