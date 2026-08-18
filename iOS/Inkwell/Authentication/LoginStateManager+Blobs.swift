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

        let boundary = "inkwell-upload-\(Int(Date().timeIntervalSince1970 * 1000))"
        let url = pdsURL.appendingPathComponent(sharedXrpcRepoUploadBlob())

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"upload\"; filename=\"blob\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: \(mimeType)\r\n\r\n".data(using: .utf8)!)
        body.append(data)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        request.httpBody = body

        let (responseData, response) = try await authenticator.response(for: request)

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            throw LoginError.httpError(status: status)
        }

        return try JSONDecoder().decode(ComAtprotoLexicon.Repository.UploadBlobOutput.self, from: responseData)
    }
}
