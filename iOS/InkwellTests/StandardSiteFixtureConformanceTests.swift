//
//  StandardSiteFixtureConformanceTests.swift
//  Inkwell
//
//  Decoder, encoder round-trip, and unknown-field preservation tests against
//  the canonical Standard.site fixtures pinned in
//  lexicons/standard-site/fixtures/ (see tools/lexicons/update-standard-site.mjs
//  for how they're pinned and validated against the checked-in Lexicon
//  schemas). Android exercises the identical JSON files through
//  kotlinx.serialization in
//  Android/app/src/test/.../data/model/atproto/StandardSiteFixtureConformanceTest.kt.
//
//  Both platforms read the *same* files rather than each hand-transcribing
//  its own copy of "what a Standard.site record looks like" -- see issue #65 --
//  so a wire-format regression on either platform's decoder shows up here
//  against real, schema-validated data instead of only against whatever a
//  test author happened to type inline.
//

import Foundation
import XCTest
import ATProtoKit
@testable import Inkwell

final class StandardSiteFixtureConformanceTests: XCTestCase {

    // MARK: - Fixture loading

    /// Locates `lexicons/standard-site/fixtures/` by walking up from this
    /// source file's own on-disk path (captured at compile time via
    /// `#filePath`), rather than by hand-copying the JSON into the test
    /// bundle. That would create exactly the second transcription this test
    /// suite exists to catch drift in.
    private static func fixturesRoot(from file: String = #filePath) -> URL {
        var directory = URL(fileURLWithPath: file).deletingLastPathComponent()
        for _ in 0..<8 {
            let candidate = directory.appendingPathComponent("lexicons/standard-site/fixtures", isDirectory: true)
            if FileManager.default.fileExists(atPath: candidate.path) {
                return candidate
            }
            directory.deleteLastPathComponent()
        }
        fatalError("Could not locate lexicons/standard-site/fixtures above \(file)")
    }

    private func fixtureData(_ relativePath: String) throws -> Data {
        try Data(contentsOf: Self.fixturesRoot().appendingPathComponent(relativePath))
    }

    private func fixtureJSON(_ relativePath: String) throws -> [String: CodableValue] {
        try JSONDecoder().decode([String: CodableValue].self, from: fixtureData(relativePath))
    }

    // MARK: - site.standard.publication

    func testDecodesMinimalPublicationFixture() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.PublicationRecord.self,
            from: fixtureData("publication.minimal.json")
        )

        XCTAssertEqual(record.url, "https://example.publication.test")
        XCTAssertEqual(record.name, "A publication with only the required fields")
        XCTAssertNil(record.description)
        XCTAssertNil(record.basicTheme)
    }

    func testDecodesFullPublicationFixtureAndEncoderRoundTrips() throws {
        let data = try fixtureData("publication.full.json")
        let record = try JSONDecoder().decode(SiteStandardLexicon.PublicationRecord.self, from: data)

        XCTAssertEqual(record.url, "https://standard.site")
        XCTAssertEqual(record.name, "Standard.site")
        XCTAssertEqual(record.icon?.mimeType, "image/png")
        XCTAssertEqual(record.icon?.size, 1696)
        XCTAssertEqual(record.basicTheme?.background, .init(r: 255, g: 255, b: 255))
        XCTAssertEqual(record.basicTheme?.foreground, .init(r: 16, g: 16, b: 16))
        XCTAssertEqual(record.basicTheme?.accent, .init(r: 0, g: 102, b: 204))
        XCTAssertEqual(record.basicTheme?.accentForeground, .init(r: 255, g: 255, b: 255))
        XCTAssertEqual(record.labels?.values.map(\.value), ["!no-unauthenticated"])
        XCTAssertEqual(record.preferences?.showInDiscover, true)

        // Encoder round trip: every field this test just asserted on must
        // survive an encode -> decode cycle unchanged.
        let reencoded = try JSONEncoder().encode(record)
        let decodedAgain = try JSONDecoder().decode(SiteStandardLexicon.PublicationRecord.self, from: reencoded)
        XCTAssertEqual(record, decodedAgain)
    }

    // MARK: - site.standard.document

    func testDecodesMinimalDocumentFixture() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.DocumentRecord.self,
            from: fixtureData("document.minimal.json")
        )

        XCTAssertEqual(record.site, "https://example.publication.test")
        XCTAssertEqual(record.title, "A loose document with only the required fields")
        XCTAssertNil(record.path)
        XCTAssertNil(record.contributors)
    }

    func testDecodesFullDocumentFixtureAndEncoderRoundTrips() throws {
        let data = try fixtureData("document.full.json")
        let record = try JSONDecoder().decode(SiteStandardLexicon.DocumentRecord.self, from: data)

        XCTAssertEqual(record.site, "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.publication/3me5vykp6lf2y")
        XCTAssertEqual(record.title, "Every field Inkwell claims to preserve")
        XCTAssertEqual(record.path, "/conformance/document-full")
        XCTAssertEqual(record.textContent, "Every field Inkwell claims to preserve.\n\nPlaintext has no markdown.")
        XCTAssertEqual(record.tags, ["conformance", "lexicon", "standard.site"])
        XCTAssertEqual(record.coverImage?.mimeType, "image/png")
        XCTAssertEqual(record.coverImage?.size, 113_475)
        XCTAssertNotNil(record.content) // Open union (Leaflet linearDocument); see ContentFormatRegistration.
        XCTAssertNotNil(record.links) // Open union; Standard.site leaves the concrete shape unspecified.
        XCTAssertEqual(record.labels?.values.map(\.value), ["!no-unauthenticated"])
        XCTAssertEqual(record.contributors?.count, 2)
        XCTAssertEqual(record.contributors?.first?.did, "did:plc:re3ebnp5v7ffagz6rb6xfei4")
        XCTAssertEqual(record.contributors?.first?.role, "editor")
        XCTAssertEqual(record.contributors?.first?.displayName, "Standard.site")
        XCTAssertEqual(record.contributors?.last?.did, "did:plc:z72i7hdynmk6r22z27h6tvur")
        XCTAssertNil(record.contributors?.last?.role)
        XCTAssertEqual(record.bskyPostRef?.recordURI, "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.post/3lbvbzbpt2k2p")
        XCTAssertNotNil(record.updatedAt)

        // Encoder round trip over every field this test just asserted on.
        //
        // `links` is deliberately excluded from the blanket struct-equality
        // check below: ATProtoKit's `UnknownType` cannot round-trip a
        // dictionary-shaped unknown value back through its own Encodable
        // implementation (re-encoding it does not reproduce a decodable
        // nested object). This is a vendored-dependency limitation, not
        // something Inkwell's own preservation guarantee relies on --
        // production edits merge unknown fields as raw JSON dictionaries via
        // `preservingUnknownFields`/`asCodableValue()`, never by re-encoding
        // a whole `DocumentRecord` through `Encodable`. Standard.site itself
        // leaves the concrete `links` shape unspecified (see the assertion
        // above), so asserting presence rather than byte-for-byte identity
        // here is the meaningful guarantee.
        let reencoded = try JSONEncoder().encode(record)
        let decodedAgain = try JSONDecoder().decode(SiteStandardLexicon.DocumentRecord.self, from: reencoded)
        XCTAssertNotNil(decodedAgain.links)
        XCTAssertEqual(record.site, decodedAgain.site)
        XCTAssertEqual(record.title, decodedAgain.title)
        XCTAssertEqual(record.path, decodedAgain.path)
        XCTAssertEqual(record.textContent, decodedAgain.textContent)
        XCTAssertEqual(record.tags, decodedAgain.tags)
        XCTAssertEqual(record.coverImage, decodedAgain.coverImage)
        XCTAssertEqual(record.labels?.values.map(\.value), decodedAgain.labels?.values.map(\.value))
        XCTAssertEqual(record.contributors, decodedAgain.contributors)
        XCTAssertEqual(record.bskyPostRef?.recordURI, decodedAgain.bskyPostRef?.recordURI)
        XCTAssertEqual(record.updatedAt, decodedAgain.updatedAt)
    }

    /// `document.extensions.json` carries `theme`, `preferences`, and
    /// `canonicalUrl` -- none declared by the pinned `site.standard.document`
    /// Lexicon (see lexicons/standard-site/inkwell-extensions.json). iOS
    /// models `theme` but not `preferences` or `canonicalUrl`, so a typed
    /// decode is expected to keep the former and silently drop the latter
    /// two -- that asymmetry is the whole reason the extensions file exists,
    /// not a bug this test should flag.
    func testDecodesDocumentExtensionsFixtureKeepingModelledFieldsOnly() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.DocumentRecord.self,
            from: fixtureData("document.extensions.json")
        )

        XCTAssertEqual(record.title, "Recommend Lexicon")
        XCTAssertNotNil(record.theme, "theme is modelled by iOS -- see inkwell-extensions.json")
        XCTAssertEqual(
            record.theme?.backgroundColor,
            .init(type: "pub.leaflet.theme.color#rgb", r: 255, g: 250, b: 240)
        )

        // preferences/canonicalUrl aren't in DocumentRecord's CodingKeys at
        // all, so there's no `record.preferences`/`record.canonicalUrl` to
        // assert against -- decoding into the typed model can't retain what
        // it doesn't declare. The raw dictionary still has them, though:
        let raw = try fixtureJSON("document.extensions.json")
        XCTAssertNotNil(raw["preferences"])
        XCTAssertNotNil(raw["canonicalUrl"])
    }

    // MARK: - site.standard.graph.subscription / site.standard.graph.recommend

    func testDecodesGraphSubscriptionFixtureAndEncoderRoundTrips() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.Graph.SubscriptionRecord.self,
            from: fixtureData("graph.subscription.json")
        )

        XCTAssertEqual(record.publication, "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.publication/3me5vykp6lf2y")
        XCTAssertNotNil(record.createdAt)

        let reencoded = try JSONEncoder().encode(record)
        let decodedAgain = try JSONDecoder().decode(SiteStandardLexicon.Graph.SubscriptionRecord.self, from: reencoded)
        XCTAssertEqual(record, decodedAgain)
    }

    func testDecodesGraphRecommendFixtureAndEncoderRoundTrips() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.Graph.RecommendRecord.self,
            from: fixtureData("graph.recommend.json")
        )

        XCTAssertEqual(record.document, "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.document/3me5vykp6lf2z")
        XCTAssertNotNil(record.createdAt)

        let reencoded = try JSONEncoder().encode(record)
        let decodedAgain = try JSONDecoder().decode(SiteStandardLexicon.Graph.RecommendRecord.self, from: reencoded)
        XCTAssertEqual(record, decodedAgain)
    }

    // MARK: - site.standard.theme.basic

    func testDecodesThemeBasicFixtureAndEncoderRoundTrips() throws {
        let record = try JSONDecoder().decode(
            SiteStandardLexicon.Theme.BasicDefinition.self,
            from: fixtureData("theme.basic.json")
        )

        XCTAssertEqual(record.background, .init(r: 14, g: 14, b: 18))
        XCTAssertEqual(record.foreground, .init(r: 236, g: 236, b: 238))
        XCTAssertEqual(record.accent, .init(r: 120, g: 180, b: 255))
        XCTAssertEqual(record.accentForeground, .init(r: 0, g: 0, b: 0))

        let reencoded = try JSONEncoder().encode(record)
        let decodedAgain = try JSONDecoder().decode(SiteStandardLexicon.Theme.BasicDefinition.self, from: reencoded)
        XCTAssertEqual(record, decodedAgain)
    }

    // MARK: - site.standard.auth{Full,Social} (derived permission-set fixture)

    /// `auth.permission-sets.json` is derived by
    /// tools/lexicons/update-standard-site.mjs straight from the pinned
    /// authFull/authSocial permission-set Lexicons, not hand-authored -- so
    /// this asserts the platforms' own OAuth scope/collection expectations
    /// against what Standard.site's Lexicons actually grant, not against a
    /// second hand-transcription of them.
    func testAuthPermissionSetsFixtureMatchesKnownScopeGrants() throws {
        let raw = try fixtureJSON("auth.permission-sets.json")
        guard case let .dictionary(permissionSets)? = raw["permissionSets"] else {
            return XCTFail("auth.permission-sets.json has no permissionSets object")
        }

        guard case let .dictionary(authFull)? = permissionSets["site.standard.authFull"] else {
            return XCTFail("Missing site.standard.authFull")
        }
        guard case let .array(authFullCollections)? = authFull["collections"] else {
            return XCTFail("site.standard.authFull has no collections array")
        }
        XCTAssertEqual(
            Set(authFullCollections.compactMap { if case let .string(value) = $0 { value } else { nil } }),
            [
                "site.standard.document",
                "site.standard.graph.recommend",
                "site.standard.graph.subscription",
                "site.standard.publication",
            ]
        )

        guard case let .dictionary(authSocial)? = permissionSets["site.standard.authSocial"] else {
            return XCTFail("Missing site.standard.authSocial")
        }
        guard case let .array(authSocialCollections)? = authSocial["collections"] else {
            return XCTFail("site.standard.authSocial has no collections array")
        }
        XCTAssertEqual(
            Set(authSocialCollections.compactMap { if case let .string(value) = $0 { value } else { nil } }),
            ["site.standard.graph.recommend", "site.standard.graph.subscription"]
        )
    }

    // MARK: - Future-field preservation regression (issue #65)

    /// `fixtures/regression/document.future-field.json` carries a property
    /// (`futureField`) that no pinned Lexicon declares -- deliberately, as a
    /// stand-in for whatever Standard.site adds next. It lives outside
    /// `fixtures/` (which `update-standard-site.mjs`'s conformance check
    /// scans) precisely because no schema will ever legitimately declare it.
    private func regressionFixtureData(_ name: String) throws -> Data {
        try Data(
            contentsOf: Self.fixturesRoot()
                .appendingPathComponent("regression", isDirectory: true)
                .appendingPathComponent(name)
        )
    }

    /// Exercises `preservingUnknownFields` directly (the function the Writer
    /// actually calls on publish -- see `WriterViewModel.swift`) against the
    /// canonical future-field fixture rather than a hand-typed literal
    /// dictionary, proving the merge helper itself keeps a field it has
    /// never seen the name of.
    func testPreservingUnknownFieldsKeepsAFieldNoLexiconDeclares() throws {
        let existingRaw = try JSONDecoder().decode(
            [String: CodableValue].self,
            from: regressionFixtureData("document.future-field.json")
        )
        let existing = UnknownType.unknown(existingRaw)

        // Simulate an edit: a client that has never heard of `futureField`
        // (correctly) only sends the fields it knows how to write.
        let updated = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "site": existingRaw["site"] ?? .string(""),
            "title": .string("Edited title"),
            "publishedAt": existingRaw["publishedAt"] ?? .string(""),
        ])

        let merged = preservingUnknownFields(from: existing, with: updated)
        let mergedFields = try merged.asCodableValue()

        XCTAssertEqual(mergedFields["title"], .string("Edited title"))
        XCTAssertEqual(
            mergedFields["futureField"],
            .string("This value must survive an edit round-trip even though no client models it yet.")
        )
        XCTAssertNotNil(mergedFields["$comment"], "Even a stray non-Lexicon key should survive untouched.")
    }

    /// The stricter, whole-pipeline version of the test above: it captures
    /// the "existing record" exactly the way the Writer really does --
    /// `LoginStateManager+Records.swift`'s `getRepositoryRecord` decodes the
    /// PDS's `value` as `UnknownType`, and `site.standard.document` is
    /// registered with `ATRecordTypeRegistry` at launch (see
    /// `SiteStandardRegistration.swift`), so that decode resolves to
    /// `.record(DocumentRecord)` -- not `.unknown(dictionary)` -- for any
    /// real document.
    ///
    /// `DocumentRecord.init(from:)` only reads the keys in its own
    /// `CodingKeys`, so `futureField` (and, today, the still-undeclared
    /// `canonicalUrl`) never survives that decode to begin with:
    /// `preservingUnknownFields`'s merge can't resurrect what the registry
    /// already threw away. `WriterMetadataPreservationTests.swift` doesn't
    /// catch this because it constructs `.unknown([...])` directly, which
    /// only happens for a genuinely *unregistered* `$type` -- never for
    /// `site.standard.document` in this app.
    ///
    /// Wrapped in `XCTExpectFailure` rather than fixed here: the fix belongs
    /// in the Writer's document-loading path (out of scope for this
    /// conformance-testing change), and this keeps the gap tracked instead of
    /// silently reintroducing it or leaving it undocumented.
    func testEditingThroughTheRegisteredDecoderDropsUndeclaredFields() throws {
        XCTExpectFailure("""
            iOS's getRepositoryRecord decodes `value` as UnknownType, which resolves to \
            .record(DocumentRecord) for the registered site.standard.document type -- so any \
            field DocumentRecord doesn't declare (like futureField here, or canonicalUrl today) \
            is already gone before preservingUnknownFields runs. See issue #65.
            """)

        let existing = try JSONDecoder().decode(
            UnknownType.self,
            from: regressionFixtureData("document.future-field.json")
        )
        // Confirms this really does take the registered-record path, not the
        // raw-dictionary fallback -- otherwise this test would be asserting
        // nothing meaningful.
        guard case .record = existing else {
            return XCTFail("Expected site.standard.document to resolve via the type registry")
        }

        let updated = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "site": .string("at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.publication/3me5vykp6lf2y"),
            "title": .string("Edited title"),
            "publishedAt": .string("2026-09-01T00:00:00.000Z"),
        ])

        let merged = preservingUnknownFields(from: existing, with: updated)
        let mergedFields = try merged.asCodableValue()

        XCTAssertEqual(
            mergedFields["futureField"],
            .string("This value must survive an edit round-trip even though no client models it yet.")
        )
    }
}
