//
//  LoginStateManager+Moderation.swift
//  Inkwell
//

import Foundation
import ATProtoKit

extension LoginStateManager {

    // MARK: - Moderation (com.atproto.moderation.createReport)

    /// Submits a moderation report to the Bluesky AppView moderation service.
    ///
    /// - Parameters:
    ///   - subject: The AT-URI (for a record) or DID (for an account) being reported.
    ///   - recordCID: CID of the record being reported; required when `subject` is an AT-URI.
    ///   - reasonType: The reason category for the report.
    ///   - reason: Optional free-text context from the user.
    func submitReport(
        subject: String,
        recordCID: String?,
        reasonType: ReportReasonType,
        reason: String
    ) async throws {
        if TestingMode.isEnabled {
            TestingModeNotice.shared.report("Submit report")
            throw LoginError.testingMode
        }

        guard resolvedPDSURL != nil else {
            throw LoginError.notAuthenticated
        }

        // Build the subject payload.
        let subjectPayload: [String: String]
        if subject.hasPrefix("did:") {
            subjectPayload = [
                "$type": "com.atproto.admin.defs#repoRef",
                "did": subject
            ]
        } else {
            guard let cid = recordCID, !cid.isEmpty else {
                throw LoginError.invalidURI
            }
            subjectPayload = [
                "$type": "com.atproto.repo.strongRef",
                "uri": subject,
                "cid": cid
            ]
        }

        let trimmedReason = reason.trimmingCharacters(in: .whitespacesAndNewlines)
        var body: [String: Any] = [
            "reasonType": reasonType.atProtoValue.rawValue,
            "subject": subjectPayload
        ]
        if !trimmedReason.isEmpty {
            body["reason"] = trimmedReason
        }

        let bodyData = try JSONSerialization.data(withJSONObject: body)
        _ = try await authenticatedData(
            path: "/xrpc/com.atproto.moderation.createReport",
            method: "POST",
            body: bodyData
        )
    }
}
