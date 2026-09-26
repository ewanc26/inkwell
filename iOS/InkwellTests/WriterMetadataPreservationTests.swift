import XCTest
import ATProtoKit
@testable import Inkwell

final class WriterMetadataPreservationTests: XCTestCase {
    func testUpdatedFieldsOverrideWhileUnknownFieldsSurvive() throws {
        let existing = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "customMetadata": .string("keep me"),
            "title": .string("old")
        ])
        let updated = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "title": .string("new")
        ])

        let merged = preservingUnknownFields(from: existing, with: updated)
        let fields = try merged.asCodableValue()

        XCTAssertEqual(fields["title"], .string("new"))
        XCTAssertEqual(fields["customMetadata"], .string("keep me"))
    }

    func testPublishedAtRemainsStableWhileEditFieldsChange() throws {
        let existing = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "publishedAt": .string("2026-01-01T00:00:00Z"),
        ])
        let updated = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "publishedAt": .string("2026-09-21T00:00:00Z"),
            "title": .string("edited"),
        ])

        let merged = preservingUnknownFields(from: existing, with: updated)
        let fields = try merged.asCodableValue()

        XCTAssertEqual(fields["publishedAt"], .string("2026-01-01T00:00:00Z"))
        XCTAssertEqual(fields["title"], .string("edited"))
    }

    func testClearedOwnedMetadataIsRemovedWhileUnownedFieldsSurvive() throws {
        let existing = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "tags": .array([.string("old")]),
            "links": .array([.string("keep me")]),
        ])
        let updated = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "title": .string("edited"),
        ])

        let merged = preservingUnknownFields(
            from: existing,
            with: updated,
            clearingAbsent: DocumentRecordComposer.metadataKeys
        )
        let fields = try merged.asCodableValue()

        XCTAssertNil(fields["tags"])
        XCTAssertEqual(fields["links"], .array([.string("keep me")]))
    }

    func testComposerWritesMetadataFromDraft() throws {
        var draft = DocumentDraft(
            site: "at://did:plc:example/site.standard.publication/self",
            title: "Title",
            publishedAt: Date(timeIntervalSince1970: 0)
        )
        draft.tags = DocumentRecordComposer.normalizedTags([" #swift ", "swift", "", "kmp"])
        draft.labels = DocumentRecordComposer.selfLabels(["nudity", "nudity"])
        draft.contributors = DocumentRecordComposer.contributors([
            ContributorDraft(did: " did:plc:editor ", role: "editor", displayName: " "),
            ContributorDraft(did: "  "),
        ])
        draft.bskyPostRef = .init(recordURI: "at://did:plc:example/app.bsky.feed.post/3k", cidHash: "bafy")

        let record = DocumentRecordComposer.record(draft: draft, content: nil, textContent: nil)
        let object = try XCTUnwrap(
            JSONSerialization.jsonObject(with: JSONEncoder().encode(record)) as? [String: Any]
        )

        XCTAssertEqual(object["tags"] as? [String], ["swift", "kmp"])
        let contributors = try XCTUnwrap(object["contributors"] as? [[String: Any]])
        XCTAssertEqual(contributors.count, 1)
        XCTAssertEqual(contributors.first?["did"] as? String, "did:plc:editor")
        XCTAssertNil(contributors.first?["displayName"])
        let labels = try XCTUnwrap(object["labels"] as? [String: Any])
        XCTAssertEqual(labels["$type"] as? String, "com.atproto.label.defs#selfLabels")
        let values = try XCTUnwrap(labels["values"] as? [[String: Any]])
        XCTAssertEqual(values.map { $0["val"] as? String }, ["nudity"])
        XCTAssertNotNil(object["bskyPostRef"])
    }

    func testEmptyMetadataStaysAbsent() {
        XCTAssertNil(DocumentRecordComposer.normalizedTags(["  ", "#"]))
        XCTAssertNil(DocumentRecordComposer.selfLabels([]))
        XCTAssertNil(DocumentRecordComposer.contributors([ContributorDraft()]))
    }
}
