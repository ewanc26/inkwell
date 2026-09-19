//
//  LoginStateManager+Blobs.swift
//  Inkwell
//

import Foundation
import ATProtoKit
import OAuthenticator

extension LoginStateManager {
    // MARK: - Blob Download

    /// Downloads raw bytes of a blob by its CID from the user's PDS.
    func downloadBlob(cid: String) async throws -> Data {
        guard let did = currentDID else {
            throw LoginError.notAuthenticated
        }
        return try await downloadBlob(cid: cid, fromDID: did)
    }

    /// Downloads a blob from the PDS hosting the specified repository.
    func downloadBlob(cid: String, fromDID did: String) async throws -> Data {
        let pdsURL = try await repositoryPDSURL(for: did)

        if did == currentDID {
            return try await authenticatedData(
                path: sharedXrpcSyncGetBlob(),
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ]
            )
        } else {
            return try await unauthenticatedData(
                pdsURL: pdsURL,
                path: sharedXrpcSyncGetBlob(),
                queryItems: [
                    URLQueryItem(name: "did", value: did),
                    URLQueryItem(name: "cid", value: cid),
                ]
            )
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

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(mimeType, forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = data

        let (responseData, response) = try await authenticator.response(for: request)

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw LoginError.httpError(status: status)
        }

        return try JSONDecoder().decode(ComAtprotoLexicon.Repository.UploadBlobOutput.self, from: responseData)
    }
}
