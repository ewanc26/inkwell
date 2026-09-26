//
//  LoginStateManager+Blobs.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit
import OAuthenticator

private let maxReaderBlobBytes = 10 * 1024 * 1024

private enum BlobDownloadError: LocalizedError {
    case oversized

    var errorDescription: String? {
        "This document's blob is too large to display safely."
    }
}

extension LoginStateManager {
    // MARK: - Blob Download

    /// Downloads raw bytes of a blob by its CID from the user's PDS.
    func downloadBlob(cid: String, declaredSize: Int? = nil) async throws -> Data {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }
        return try await downloadBlob(cid: cid, fromDID: did, declaredSize: declaredSize)
    }

    /// Downloads a blob from the PDS hosting the specified repository.
    func downloadBlob(cid: String, fromDID did: String, declaredSize: Int? = nil) async throws -> Data {
        try validateDeclaredBlobSize(declaredSize)
        let pdsURL = try await repositoryPDSURL(for: did)

        if did == currentDID {
            let data = try await authenticatedData(
                path: sharedXrpcSyncGetBlob(),
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ],
                expectJSON: false,
                maxResponseBytes: maxReaderBlobBytes
            )
            guard data.count <= maxReaderBlobBytes else { throw BlobDownloadError.oversized }
            return data
        } else {
            let data = try await unauthenticatedData(
                pdsURL: pdsURL,
                path: sharedXrpcSyncGetBlob(),
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ],
                expectJSON: false,
                maxResponseBytes: maxReaderBlobBytes
            )
            guard data.count <= maxReaderBlobBytes else { throw BlobDownloadError.oversized }
            return data
        }
    }

    // MARK: - Blob-backed Content

    /// Resolves a content object that stores its body in a blob rather than
    /// inline — Leaflet `blobPages` or markpub `textBlob` — returning content
    /// with the body inlined so the ordinary providers can read it.
    ///
    /// Returns the content untouched when the body is already readable inline
    /// (`contentBodyNeedsBlobDownload` is the matching predicate), and falls
    /// back to the original (rather than throwing) when the blob can't be
    /// fetched, so a failed download degrades instead of losing the record.
    func resolveBlobBackedContent(_ content: UnknownType?, authorDID: String? = nil) async -> UnknownType? {
        guard let content else { return nil }

        if let leaflet = content.getRecord(ofType: LeafletContent.self),
           let blobRef = leaflet.blobPages,
           (leaflet.pages?.isEmpty ?? true) {
            do {
                let blobData = try await blobData(for: blobRef, authorDID: authorDID)
                try JSONSafety.validateResponse(blobData)
                let pages = try JSONDecoder().decode([LeafletPage].self, from: blobData)
                return UnknownType.record(LeafletContent(pages: pages, blobPages: nil))
            } catch {
                logger.error("[resolveBlobBackedContent] leaflet blobPages fetch failed: \(error)")
                return content
            }
        }

        if let markpub = content.getRecord(ofType: MarkpubContent.self),
           let blobRef = markpub.text.textBlob,
           (markpub.text.markdown?.isEmpty ?? true) {
            do {
                let blobData = try await blobData(for: blobRef, authorDID: authorDID)
                let markdown = String(decoding: blobData, as: UTF8.self)
                return UnknownType.record(
                    MarkpubContent(text: MarkpubText(type: markpub.text.type, markdown: markdown))
                )
            } catch {
                logger.error("[resolveBlobBackedContent] markpub textBlob fetch failed: \(error)")
                return content
            }
        }

        return content
    }

    private func blobData(
        for blob: ComAtprotoLexicon.Repository.UploadBlobOutput,
        authorDID: String?
    ) async throws -> Data {
        if let authorDID {
            return try await downloadBlob(
                cid: blob.reference.link,
                fromDID: authorDID,
                declaredSize: blob.size
            )
        }
        return try await downloadBlob(cid: blob.reference.link, declaredSize: blob.size)
    }

    // MARK: - Blob Upload

    /// Uploads raw data as a blob to the user's PDS.
    func uploadBlob(_ data: Data, mimeType: String) async throws -> ComAtprotoLexicon.Repository.UploadBlobOutput {
        if TestingMode.isEnabled {
            TestingModeNotice.shared.report("Upload image")
            throw LoginError.testingMode
        }
        guard let authenticator, let pdsURL = resolvedPDSURL else {
            throw LoginError.notAuthenticated
        }

        let url = pdsURL.appendingPathComponent(sharedXrpcRepoUploadBlob())

        let request = makeBlobUploadRequest(url: url, data: data, mimeType: mimeType)

        let (responseData, response) = try await authenticator.response(for: request)

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw LoginError.httpError(status: status)
        }

        try JSONSafety.validateResponse(responseData)
        let blob = try JSONDecoder().decode(ComAtprotoLexicon.Repository.UploadBlobOutput.self, from: responseData)
        try validateUploadedBlob(blob, expectedMimeType: mimeType, expectedSize: data.count)
        return blob
    }
}

/// Builds the protocol-level upload request independently of authentication.
/// AT Protocol uploadBlob receives the exact blob bytes, not multipart framing.
func makeBlobUploadRequest(url: URL, data: Data, mimeType: String) -> URLRequest {
    var request = URLRequest(url: url)
    request.httpMethod = "POST"
    request.setValue(mimeType, forHTTPHeaderField: "Content-Type")
    request.setValue(String(data.count), forHTTPHeaderField: "Content-Length")
    request.setValue("application/json", forHTTPHeaderField: "Accept")
    request.httpBody = data
    return request
}

func validateDeclaredBlobSize(_ declaredSize: Int?) throws {
    if let declaredSize, declaredSize < 0 || declaredSize > maxReaderBlobBytes {
        throw BlobDownloadError.oversized
    }
}

func validateUploadedBlob(
    _ blob: ComAtprotoLexicon.Repository.UploadBlobOutput,
    expectedMimeType: String,
    expectedSize: Int
) throws {
    guard blob.size == expectedSize,
          blob.mimeType == expectedMimeType,
          !blob.reference.link.isEmpty else {
        throw LoginError.httpError(status: 0)
    }
}
