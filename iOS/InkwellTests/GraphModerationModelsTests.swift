//
//  GraphModerationModelsTests.swift
//  Inkwell
//

import Foundation
import XCTest
@testable import Inkwell

@MainActor
final class GraphModerationModelsTests: XCTestCase {
    func testBlockRecordRoundTripsWireShape() throws {
        let createdAt = Date(timeIntervalSince1970: 1_787_652_000)
        let record = AppBskyGraphBlockRecord(
            subject: "did:plc:exampleblockedactor",
            createdAt: createdAt
        )

        let data = try JSONEncoder().encode(record)
        let object = try XCTUnwrap(
            JSONSerialization.jsonObject(with: data) as? [String: Any]
        )

        XCTAssertEqual(object["$type"] as? String, "app.bsky.graph.block")
        XCTAssertEqual(object["subject"] as? String, "did:plc:exampleblockedactor")
        XCTAssertNotNil(object["createdAt"] as? String)

        let decoded = try JSONDecoder().decode(AppBskyGraphBlockRecord.self, from: data)
        XCTAssertEqual(decoded.subject, record.subject)
        XCTAssertEqual(decoded.createdAt.timeIntervalSince1970, createdAt.timeIntervalSince1970, accuracy: 0.001)
    }
}
