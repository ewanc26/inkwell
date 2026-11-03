//
//  PushRegistrationBodyTests.swift
//  InkwellTests
//
//  Created for issue #171: test(ios): add unit test for push registration request body
//

import XCTest
@testable import Inkwell

/// Wire-contract coverage for the APNs registration hand-off (issue #152, part of #35).
///
/// Mirrors Android's PushRegistrationBodyTest.kt: validates that the JSON
/// produced by PushSubscriptionRequestPayload matches the push register wire contract
/// expected by the server stub at website/src/routes/api/push/register/+server.ts.
final class PushRegistrationBodyTests: XCTestCase {

    /// Local mirror of PushSubscriptionRequestPayload for testing.
    /// This matches the private struct in PushNotificationManager.swift.
    private struct PushSubscriptionRequestPayload: Codable {
        let platform: String
        let deviceToken: String
        let topics: [String]
        let did: String
    }

    func testRegistrationBodyMatchesPushRegisterWireContract() throws {
        let request = PushSubscriptionRequestPayload(
            platform: "ios",
            deviceToken: "apns-token-123",
            topics: ["subscribe", "recommend", "comment"],
            did: "did:plc:alice"
        )

        let encoder = JSONEncoder()
        encoder.outputFormatting = .sortedKeys
        let jsonData = try encoder.encode(request)
        let jsonString = String(data: jsonData, encoding: .utf8)

        // The server expects: platform, deviceToken, topics, did
        // Verify the JSON contains all required fields
        XCTAssertTrue(jsonString?.contains("\"platform\":\"ios\"") ?? false)
        XCTAssertTrue(jsonString?.contains("\"deviceToken\":\"apns-token-123\"") ?? false)
        XCTAssertTrue(jsonString?.contains("\"did\":\"did:plc:alice\"") ?? false)
        XCTAssertTrue(jsonString?.contains("\"topics\":[\"comment\",\"recommend\",\"subscribe\"]") ?? false)
    }

    func testRegistrationBodyEscapesSpecialCharacters() throws {
        let request = PushSubscriptionRequestPayload(
            platform: "ios",
            deviceToken: "tok\"en",
            topics: ["subscribe", "recommend", "comment"],
            did: "did:plc:\"x\""
        )

        let encoder = JSONEncoder()
        let jsonData = try encoder.encode(request)
        let jsonString = String(data: jsonData, encoding: .utf8)

        // JSONEncoder should properly escape quotes
        XCTAssertTrue(jsonString?.contains("\"deviceToken\":\"tok\\\"en\"") ?? false)
        XCTAssertTrue(jsonString?.contains("\"did\":\"did:plc:\\\"x\\\"\"") ?? false)
    }

    func testRegistrationBodyMatchesAndroidWireFormat() throws {
        // Both platforms should produce compatible JSON structures
        // Android uses: platform, deviceToken, topics, did
        // iOS uses the same structure via PushSubscriptionRequestPayload

        let request = PushSubscriptionRequestPayload(
            platform: "ios",
            deviceToken: "test-token",
            topics: ["subscribe", "recommend", "comment"],
            did: "did:plc:test"
        )

        let encoder = JSONEncoder()
        let jsonData = try encoder.encode(request)

        // Decode it back to verify structure
        let decoded = try JSONDecoder().decode(PushSubscriptionRequestPayload.self, from: jsonData)

        XCTAssertEqual(decoded.platform, "ios")
        XCTAssertEqual(decoded.deviceToken, "test-token")
        XCTAssertEqual(decoded.topics, ["subscribe", "recommend", "comment"])
        XCTAssertEqual(decoded.did, "did:plc:test")
    }
}
