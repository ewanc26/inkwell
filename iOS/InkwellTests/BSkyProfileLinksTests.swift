//
//  BSkyProfileLinksTests.swift
//  Inkwell
//
//  Verifies `BSkyActorProfile` decodes the `links` carried by a
//  `app.bsky.actor.defs#profileViewDetailed` (`app.bsky.actor.getProfile`).
//  List subjects decode as `profileView`, which has no `links` — so the
//  field must also tolerate being absent entirely.
//  Mirrors Android's `BlueskyProfileLinksTest`.
//
//

import Foundation
import XCTest
@testable import Inkwell

// `@MainActor` matches `BSkyListModelsTests` — the app target's model
// conformances are main-actor-isolated, so decoding them from a
// nonisolated test context is an error in the Swift 6 language mode.
@MainActor
final class BSkyProfileLinksTests: XCTestCase {

    /// `profileViewDetailed` shape with the fields the app doesn't declare
    /// (`pronouns`, deprecated `website`, `joinedViaStarterPack`, ...) left
    /// in, which decoding must tolerate.
    private let fixture = """
    {
      "did": "did:plc:example",
      "handle": "example.bsky.social",
      "displayName": "Example",
      "description": "Testing links",
      "pronouns": "they/them",
      "website": "https://example.com/old",
      "avatar": "https://cdn.bsky.app/img/avatar/plain/did:plc:example/bafkreiexample",
      "followersCount": 42,
      "links": [
        {
          "uri": "at://did:plc:example/app.bsky.actor.link/3mexample1",
          "cid": "bafyreexample1",
          "url": "https://example.com",
          "title": "Example site",
          "icon": "https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:example/bafkreiexample",
          "labels": []
        },
        {
          "uri": "at://did:plc:example/app.bsky.actor.link/3mexample2",
          "cid": "bafyreexample2",
          "url": "https://untitled.example"
        }
      ],
      "labels": [],
      "createdAt": "2023-04-26T05:22:14.855Z",
      "indexedAt": "2026-07-22T10:12:57.160Z"
    }
    """

    func testDecodesProfileLinksInDisplayOrder() throws {
        let decoded = try JSONDecoder().decode(
            BSkyActorProfile.self,
            from: Data(fixture.utf8)
        )

        let links = try XCTUnwrap(decoded.links)
        XCTAssertEqual(links.count, 2)
        XCTAssertEqual(links[0].url, "https://example.com")
        XCTAssertEqual(links[0].title, "Example site")
        XCTAssertEqual(links[1].url, "https://untitled.example")
        XCTAssertNil(links[1].title)
    }

    func testMissingLinksDecodesAsNilForProfileViewSubjects() throws {
        let decoded = try JSONDecoder().decode(
            BSkyActorProfile.self,
            from: Data(#"{"did":"did:plc:example","handle":"example.bsky.social"}"#.utf8)
        )

        XCTAssertNil(decoded.links)
    }
}
