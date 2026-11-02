//
//  BSkyProfileDetailsTests.swift
//  Inkwell
//
//  Verifies `BSkyActorProfile` decodes the `profileViewDetailed`-only
//  presentation fields (`app.bsky.actor.getProfile`): pronouns today, with
//  verification and the pinned post to follow. List subjects decode as
//  `profileView`, which carries none of these — so each must tolerate being
//  absent.
//  Mirrors Android's `BlueskyProfileDetailsTest`.
//
//

import Foundation
import XCTest
@testable import Inkwell

// `@MainActor` matches `BSkyListModelsTests` — the app target's model
// conformances are main-actor-isolated, so decoding them from a
// nonisolated test context is an error in the Swift 6 language mode.
@MainActor
final class BSkyProfileDetailsTests: XCTestCase {

    private let fixture = """
    {
      "did": "did:plc:example",
      "handle": "example.bsky.social",
      "displayName": "Example",
      "pronouns": "they/them",
      "description": "Testing details",
      "labels": [],
      "createdAt": "2023-04-26T05:22:14.855Z",
      "indexedAt": "2026-07-22T10:12:57.160Z"
    }
    """

    func testDecodesDetailedProfilePresentationFields() throws {
        let decoded = try JSONDecoder().decode(
            BSkyActorProfile.self,
            from: Data(fixture.utf8)
        )

        XCTAssertEqual(decoded.pronouns, "they/them")
    }

    func testMissingPresentationFieldsDecodeAsNilForProfileViewSubjects() throws {
        let decoded = try JSONDecoder().decode(
            BSkyActorProfile.self,
            from: Data(#"{"did":"did:plc:example","handle":"example.bsky.social"}"#.utf8)
        )

        XCTAssertNil(decoded.pronouns)
    }
}
