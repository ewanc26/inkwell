//
//  LoginStateManager+Types.swift
//  Inkwell
//

import Foundation
import ATProtoKit

// MARK: - Supporting Types

/// A minimal Bluesky profile snapshot used for display-name / avatar.
struct ProfileSnapshot: Sendable {
    let displayName: String?
    let avatarURL: URL?
}

// MARK: - List Records Helpers

/// A single record entry in a `com.atproto.repo.listRecords` response page.
struct RepositoryRecord: Decodable, Sendable {
    let uri: String
    let cid: String?
    let value: UnknownType?
}

/// A `listRecords` response page that tolerates malformed records
/// (decodes what it can, drops the rest).
struct TolerantRecordPage: Decodable, Sendable {
    let cursor: String?
    let records: [RepositoryRecord]

    private enum CodingKeys: String, CodingKey {
        case cursor, records
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        cursor = try container.decodeIfPresent(String.self, forKey: .cursor)
        records = try container.decode(LossyRecordArray.self, forKey: .records).elements
    }
}

/// Decodes an array of `RepositoryRecord`, silently skipping any element
/// that fails to decode (e.g. because its `$type` is unrecognised).
private struct LossyRecordArray: Decodable, Sendable {
    let elements: [RepositoryRecord]

    init(from decoder: Decoder) throws {
        var container = try decoder.unkeyedContainer()
        var decoded: [RepositoryRecord] = []
        while !container.isAtEnd {
            let elementDecoder = try container.superDecoder()
            if let record = try? RepositoryRecord(from: elementDecoder) {
                decoded.append(record)
            }
        }
        elements = decoded
    }
}
