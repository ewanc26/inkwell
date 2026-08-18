//
//  LoginStateManager+Feedback.swift
//  Inkwell
//

import Foundation
import ATProtoKit

extension LoginStateManager {
    // MARK: - Feedback (app.userinput.discussion)
    //
    // Sends in-app feedback to Inkwell's userinput.app board (owned by
    // ewancroft.uk). The discussion record lives in the *submitting user's*
    // own repo and points at Inkwell's fixed feedback space via a strong
    // reference. Only creation is implemented; Inkwell doesn't read or
    // moderate the board.

    /// Posts feedback to Inkwell's userinput.app space. `tag`, if provided,
    /// must be one of `UserInputFeedback.tags`.
    @discardableResult
    func submitFeedback(title: String, body: String?, tag: String?) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        if TestingMode.isEnabled {
            TestingModeNotice.shared.report("Send feedback")
            throw LoginError.testingMode
        }
        guard !title.isEmpty, title.count <= UserInputFeedback.titleMaxLength else {
            throw LoginError.invalidURI
        }

        let parsedSpace = parseAtUri(UserInputFeedback.inkwellSpaceURI)
        guard let parsedSpace else { throw LoginError.invalidURI }

        // Resolve the space's current CID — a strongRef must match exactly.
        let (_, spaceCID, _) = try await getRepositoryRecord(
            from: parsedSpace.did, collection: parsedSpace.collection, recordKey: parsedSpace.recordKey
        )
        guard let spaceCID else { throw LoginError.pdsResolutionFailed }

        let trimmedBody = body?.trimmingCharacters(in: .whitespacesAndNewlines)
        let record = UnknownType.record(
            UserInputDiscussionRecord(
                title: title,
                body: (trimmedBody?.isEmpty ?? true) ? nil : trimmedBody,
                tags: tag.map { [$0] },
                space: .init(uri: UserInputFeedback.inkwellSpaceURI, cid: spaceCID)
            )
        )
        return try await createRecord(collection: UserInputDiscussionRecord.type, record: record)
    }
}
