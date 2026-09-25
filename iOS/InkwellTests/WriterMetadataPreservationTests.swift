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
}
