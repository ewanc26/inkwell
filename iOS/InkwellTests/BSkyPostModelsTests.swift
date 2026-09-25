import Foundation
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

    private func decodePost(embed: String) throws -> BSkyPostView {
        let json = #"{"uri":"at://did:plc:author/app.bsky.feed.post/post","author":{"did":"did:plc:author","handle":"author.example","displayName":"Author"},"record":{"text":"Post text","createdAt":"2026-09-21T00:00:00Z"},"embed":__EMBED__}"#.replacingOccurrences(of: "__EMBED__", with: embed)
        return try JSONDecoder().decode(BSkyPostView.self, from: Data(json.utf8))
    }
}
