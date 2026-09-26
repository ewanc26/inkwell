import Foundation
import SwiftUI
import XCTest
@testable import Inkwell

@MainActor
final class BSkyPostModelsTests: XCTestCase {
    func testDecodesImageAltTextUsedByAccessibilityRenderer() throws {
        let post = try decodePost(embed: #"{"$type":"app.bsky.embed.images","images":[{"thumb":"https://cdn.example/thumb.jpg","fullsize":"https://cdn.example/full.jpg","alt":"A red kite over the sea"}]}"#)

        guard case .images(let embed) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an image embed")
        }
        XCTAssertEqual(embed.images.first?.alt, "A red kite over the sea")
    }

    func testDecodesExternalAndQuotedEmbedContext() throws {
        let externalPost = try decodePost(embed: #"{"$type":"app.bsky.embed.external","external":{"uri":"https://example.com/story","title":"A story","description":"A description","thumb":"https://example.com/thumb.jpg"}}"#)
        guard case .external(let external) = try XCTUnwrap(externalPost.embed) else {
            return XCTFail("Expected an external embed")
        }
        XCTAssertEqual(external.external.title, "A story")
        XCTAssertEqual(external.external.description, "A description")

        let quotedPost = try decodePost(embed: #"{"$type":"app.bsky.embed.record","record":{"uri":"at://did:plc:author/app.bsky.feed.post/abc","author":{"did":"did:plc:author","handle":"author.example","displayName":"Author"},"value":{"text":"Quoted text","createdAt":"2026-09-21T00:00:00Z"}}}"#)
        guard case .record(let record) = try XCTUnwrap(quotedPost.embed) else {
            return XCTFail("Expected a quoted record embed")
        }
        XCTAssertEqual(record.record.author?.displayName, "Author")
        XCTAssertEqual(record.record.value?.text, "Quoted text")
    }

    func testDecodesEnrichedStandardSiteExternalEmbed() throws {
        let json = #"{"$type":"app.bsky.embed.external","external":{"uri":"https://example.com/doc","title":"Standard.site Post","description":"Longform post","readingTime":5,"createdAt":"2026-09-21T00:00:00Z","associatedRefs":[{"uri":"at://did:plc:author/site.standard.document/123","cid":"bafyabc"}]}}"#
        let post = try decodePost(embed: json)

        guard case .external(let external) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an external embed")
        }
        XCTAssertEqual(external.external.readingTime, 5)
        XCTAssertEqual(external.external.createdAt, "2026-09-21T00:00:00Z")
        XCTAssertEqual(external.external.associatedRefs?.first?.uri, "at://did:plc:author/site.standard.document/123")
        XCTAssertEqual(external.external.associatedRefs?.first?.cid, "bafyabc")
    }

    /// A realistic Standard.site-enriched `app.bsky.embed.external#viewExternal`
    /// payload, as returned by the Bluesky AppView after the May 2026
    /// Standard.site integration: title/description/uri/thumb (the
    /// pre-existing fields) plus readingTime/labels/source/associatedRefs/
    /// associatedProfiles (the enriched fields added by this change).
    private static let enrichedStandardSiteEmbedJSON = #"""
    {"$type":"app.bsky.embed.external","external":{
        "uri":"https://example.press/articles/tide-charts",
        "title":"Reading the Tide Charts",
        "description":"A field guide to the numbers on the harbor board.",
        "thumb":"https://example.press/cdn/tide-charts-cover.jpg",
        "createdAt":"2026-09-18T09:00:00Z",
        "updatedAt":"2026-09-20T14:30:00Z",
        "readingTime":6,
        "labels":[{"src":"did:plc:labeler","uri":"https://example.press/articles/tide-charts","val":"long-form","cts":"2026-09-18T09:00:00Z"}],
        "source":{
            "uri":"https://example.press",
            "icon":"https://example.press/cdn/icon.png",
            "title":"Example Press",
            "description":"Independent harbor journalism.",
            "theme":{
                "backgroundRGB":{"r":255,"g":255,"b":255},
                "foregroundRGB":{"r":20,"g":20,"b":20},
                "accentRGB":{"r":10,"g":90,"b":160},
                "accentForegroundRGB":{"r":255,"g":255,"b":255}
            }
        },
        "associatedRefs":[{"uri":"at://did:plc:author/site.standard.document/123","cid":"bafyabc"}],
        "associatedProfiles":[{"did":"did:plc:author","handle":"author.example","displayName":"Harbor Author","avatar":"https://example.press/cdn/avatar.jpg"}]
    }}
    """#

    func testDecodesFullyEnrichedStandardSiteExternalEmbedFixture() throws {
        let post = try decodePost(embed: Self.enrichedStandardSiteEmbedJSON)

        guard case .external(let embed) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an external embed")
        }
        let external = embed.external

        // Pre-existing fields keep decoding.
        XCTAssertEqual(external.uri, "https://example.press/articles/tide-charts")
        XCTAssertEqual(external.title, "Reading the Tide Charts")
        XCTAssertEqual(external.description, "A field guide to the numbers on the harbor board.")
        XCTAssertEqual(external.thumb, "https://example.press/cdn/tide-charts-cover.jpg")
        XCTAssertEqual(external.readingTime, 6)
        XCTAssertEqual(external.createdAt, "2026-09-18T09:00:00Z")
        XCTAssertEqual(external.updatedAt, "2026-09-20T14:30:00Z")
        XCTAssertEqual(external.associatedRefs?.first?.uri, "at://did:plc:author/site.standard.document/123")

        // Newly-decoded fields: labels, source, associatedProfiles.
        XCTAssertEqual(external.labels?.first?.val, "long-form")
        XCTAssertEqual(external.source?.title, "Example Press")
        XCTAssertEqual(external.source?.icon, "https://example.press/cdn/icon.png")
        XCTAssertEqual(external.source?.theme?.accentRGB?.r, 10)
        XCTAssertEqual(external.source?.theme?.accentRGB?.g, 90)
        XCTAssertEqual(external.source?.theme?.accentRGB?.b, 160)
        XCTAssertEqual(external.associatedProfiles?.first?.handle, "author.example")
        XCTAssertEqual(external.associatedProfiles?.first?.displayName, "Harbor Author")
    }

    func testEnrichedEmbedDisplayHelpersFormatForRendering() throws {
        let post = try decodePost(embed: Self.enrichedStandardSiteEmbedJSON)
        guard case .external(let embed) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an external embed")
        }
        let external = embed.external

        XCTAssertTrue(external.isStandardSiteEnriched)
        XCTAssertEqual(external.readingTimeLabel, "6 min read")
        XCTAssertEqual(external.contentWarningLabels, ["long-form"])
        XCTAssertEqual(external.sourceDisplayTitle, "Example Press")
        XCTAssertEqual(external.source?.theme?.accentRGB?.color, Color(red: 10.0 / 255, green: 90.0 / 255, blue: 160.0 / 255))
    }

    func testPlainExternalEmbedIsNotStandardSiteEnriched() throws {
        let post = try decodePost(embed: #"{"$type":"app.bsky.embed.external","external":{"uri":"https://example.com/story","title":"A story","description":"A description","thumb":"https://example.com/thumb.jpg"}}"#)
        guard case .external(let embed) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an external embed")
        }

        XCTAssertFalse(embed.external.isStandardSiteEnriched)
        XCTAssertNil(embed.external.readingTimeLabel)
        XCTAssertEqual(embed.external.contentWarningLabels, [])
        XCTAssertNil(embed.external.source)
        XCTAssertNil(embed.external.labels)
        XCTAssertNil(embed.external.associatedProfiles)
    }

    func testUnknownFutureFieldsDoNotBreakExternalEmbedDecoding() throws {
        let json = #"{"$type":"app.bsky.embed.external","external":{"uri":"https://example.com/story","title":"A story","description":"A description","futureField":{"nested":true},"anotherNewThing":[1,2,3]}}"#
        let post = try decodePost(embed: json)

        guard case .external(let embed) = try XCTUnwrap(post.embed) else {
            return XCTFail("Expected an external embed")
        }
        XCTAssertEqual(embed.external.title, "A story")
    }

    private func decodePost(embed: String) throws -> BSkyPostView {
        let json = #"{"uri":"at://did:plc:author/app.bsky.feed.post/post","author":{"did":"did:plc:author","handle":"author.example","displayName":"Author"},"record":{"text":"Post text","createdAt":"2026-09-21T00:00:00Z"},"embed":__EMBED__}"#.replacingOccurrences(of: "__EMBED__", with: embed)
        return try JSONDecoder().decode(BSkyPostView.self, from: Data(json.utf8))
    }
}
