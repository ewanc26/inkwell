//
//  LoginStateManager+Blobs.swift
//  Inkwell
//

import Foundation
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
        guard blob.size == data.count,
              blob.mimeType == mimeType,
              !blob.reference.link.isEmpty else {
            throw LoginError.httpError(status: 0)
        }
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
