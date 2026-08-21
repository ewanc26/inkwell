//
//  BSkyListModelsTests.swift
//  Inkwell
//
//  Verifies `GetListResponse` decodes a real `app.bsky.graph.getList`
//  response, the shape behind the "Supporters" section in CreditsView.
//  Mirrors Android's `BlueskyListModelsTest`.
//

import Foundation
import XCTest
@testable import Inkwell

// MARK: - Bluesky List Decoding Tests

// `@MainActor` matches `StandardSiteTests` — the app target's model
// conformances are main-actor-isolated, so decoding them from a
// nonisolated test context is an error in the Swift 6 language mode.
@MainActor
final class BSkyListModelsTests: XCTestCase {

    /// Fixture captured from Inkwell's own supporters list
    /// (`SupportersList.uri`, `limit=2`) — trimmed of fields the app
    /// doesn't declare, which decoding must tolerate since the real
    /// payload carries far more (pronouns, associated, labels, ...)
    /// than `BSkyActorProfile` reads.
    private let fixture = """
    {
      "items": [
        {
          "uri": "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.listitem/3mtjl3hfwhp27",
          "subject": {
            "did": "did:plc:jbeaa5kdaladzwq3r7f5xgwe",
            "handle": "danielroe.dev",
            "displayName": "danielroe",
            "pronouns": "he/him",
            "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:jbeaa5kdaladzwq3r7f5xgwe/bafkreif4d7wtmzqppbpnwhjulf3d36ltbeg5wzu3i2mhq6wxb4f6nh5uo4",
            "associated": { "chat": { "allowIncoming": "all" } },
            "labels": [],
            "createdAt": "2023-04-26T05:22:14.855Z",
            "description": "building @nuxt.com",
            "indexedAt": "2026-07-22T10:12:57.160Z"
          }
        },
        {
          "uri": "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.listitem/3mtjl2op6qx27",
          "subject": {
            "did": "did:plc:hu2jmpvtlecuwqnosnloplx6",
            "handle": "captaincalliope.at",
            "displayName": "Lyre Calliope 🧭✨",
            "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:hu2jmpvtlecuwqnosnloplx6/bafkreibmf27a7ju4bdfamv4xxszl5w7kqgapnz237y53f2xgoolcwjrnwy",
            "labels": [],
            "createdAt": "2023-11-13T03:20:03.535Z",
            "indexedAt": "2026-05-25T19:43:38.264Z"
          }
        }
      ],
      "cursor": "3mtjl2op6qx27"
    }
    """

    func testDecodesRealGetListResponseUnknownKeysAndAll() throws {
        let decoded = try JSONDecoder().decode(
            GetListResponse.self,
            from: Data(fixture.utf8)
        )

        XCTAssertEqual(decoded.items.count, 2)
        XCTAssertEqual(decoded.cursor, "3mtjl2op6qx27")

        let first = decoded.items[0].subject
        XCTAssertEqual(first.handle, "danielroe.dev")
        XCTAssertEqual(first.displayName, "danielroe")
        XCTAssertNotNil(first.avatar)

        let second = decoded.items[1].subject
        XCTAssertEqual(second.handle, "captaincalliope.at")
        XCTAssertEqual(second.displayName, "Lyre Calliope 🧭✨")
    }

    func testMissingCursorDecodesAsNilEndingPagination() throws {
        let decoded = try JSONDecoder().decode(
            GetListResponse.self,
            from: Data(#"{"items":[]}"#.utf8)
        )

        XCTAssertTrue(decoded.items.isEmpty)
        XCTAssertNil(decoded.cursor)
    }

    func testSupportersListURIMatchesSharedConstant() {
        // The list AT-URI is a duplicated literal rather than a bridged
        // shared constant, so nothing but a test keeps the two copies
        // honest. Android's copy lives in `shared/.../SupportersList.kt`.
        XCTAssertEqual(
            SupportersList.uri,
            "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27"
        )
    }
}
