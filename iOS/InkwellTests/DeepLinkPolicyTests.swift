//
//  DeepLinkPolicyTests.swift
//  Inkwell
//
//  Unit tests for the two content deep-link entry points: the trusted
//  `inkwell://document` custom scheme (ContentDeepLinkPolicy) and the
//  unverified `https://inkwell.ewancroft.uk/open` universal-link hand-off
//  (HttpsDeepLinkPolicy). Both must survive percent-encoded Unicode, path
//  segments, and extra/interleaved query parameters without mis-parsing
//  the AT-URI or, worse, accepting something that isn't a document AT-URI.
//

import XCTest
@testable import Inkwell

final class DeepLinkPolicyTests: XCTestCase {

    // MARK: - ContentDeepLinkPolicy (inkwell://document, custom scheme)

    func testContentDeepLinkAcceptsPlainDocumentURI() throws {
        let url = try XCTUnwrap(
            URL(string: "inkwell://document?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertEqual(
            ContentDeepLinkPolicy.documentURI(from: url),
            "at://did:plc:abc123/site.standard.document/xyz"
        )
    }

    func testContentDeepLinkDecodesPercentEncodedUnicodeRecordKey() throws {
        // "café" record key, percent-encoded in the query value.
        let url = try XCTUnwrap(
            URL(string: "inkwell://document?uri=at://did:plc:abc123/site.standard.document/caf%C3%A9")
        )
        XCTAssertEqual(
            ContentDeepLinkPolicy.documentURI(from: url),
            "at://did:plc:abc123/site.standard.document/café"
        )
    }

    func testContentDeepLinkIgnoresUnrelatedQueryParameters() throws {
        let url = try XCTUnwrap(
            URL(string: "inkwell://document?ref=notification&uri=at://did:plc:abc123/site.standard.document/xyz&utm_source=share")
        )
        XCTAssertEqual(
            ContentDeepLinkPolicy.documentURI(from: url),
            "at://did:plc:abc123/site.standard.document/xyz"
        )
    }

    func testContentDeepLinkRejectsNonDocumentCollection() throws {
        let url = try XCTUnwrap(
            URL(string: "inkwell://document?uri=at://did:plc:abc123/site.standard.publication/xyz")
        )
        XCTAssertNil(ContentDeepLinkPolicy.documentURI(from: url))
    }

    func testContentDeepLinkRejectsWrongHost() throws {
        let url = try XCTUnwrap(
            URL(string: "inkwell://callback?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertNil(ContentDeepLinkPolicy.documentURI(from: url))
    }

    func testContentDeepLinkRejectsMalformedURI() throws {
        let url = try XCTUnwrap(URL(string: "inkwell://document?uri=not-an-at-uri"))
        XCTAssertNil(ContentDeepLinkPolicy.documentURI(from: url))
    }

    // MARK: - HttpsDeepLinkPolicy (https://inkwell.ewancroft.uk/open, universal link)

    func testHttpsDeepLinkAcceptsOpenPathWithDocumentURI() throws {
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/open?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertEqual(
            HttpsDeepLinkPolicy.candidateDocumentURI(from: url),
            "at://did:plc:abc123/site.standard.document/xyz"
        )
    }

    func testHttpsDeepLinkAcceptsNestedOpenPathSegment() throws {
        // `/open/<something>` — a trailing path segment shouldn't break parsing.
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/open/document?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertEqual(
            HttpsDeepLinkPolicy.candidateDocumentURI(from: url),
            "at://did:plc:abc123/site.standard.document/xyz"
        )
    }

    func testHttpsDeepLinkDecodesPercentEncodedUnicodeRecordKey() throws {
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/open?uri=at://did:plc:abc123/site.standard.document/%E6%97%A5%E8%A8%98")
        )
        XCTAssertEqual(
            HttpsDeepLinkPolicy.candidateDocumentURI(from: url),
            "at://did:plc:abc123/site.standard.document/日記"
        )
    }

    func testHttpsDeepLinkIgnoresExtraQueryParametersAroundURI() throws {
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/open?url=https%3A%2F%2Fexample.com%2Farticle&uri=at://did:plc:abc123/site.standard.document/xyz&ref=email")
        )
        XCTAssertEqual(
            HttpsDeepLinkPolicy.candidateDocumentURI(from: url),
            "at://did:plc:abc123/site.standard.document/xyz"
        )
    }

    func testHttpsDeepLinkRejectsWrongHost() throws {
        let url = try XCTUnwrap(
            URL(string: "https://evil.example.com/open?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertNil(HttpsDeepLinkPolicy.candidateDocumentURI(from: url))
    }

    func testHttpsDeepLinkRejectsNonOpenPath() throws {
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/privacy?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertNil(HttpsDeepLinkPolicy.candidateDocumentURI(from: url))
    }

    func testHttpsDeepLinkRejectsHttpScheme() throws {
        // Only https:// participates in Associated Domains verification.
        let url = try XCTUnwrap(
            URL(string: "http://inkwell.ewancroft.uk/open?uri=at://did:plc:abc123/site.standard.document/xyz")
        )
        XCTAssertNil(HttpsDeepLinkPolicy.candidateDocumentURI(from: url))
    }

    func testHttpsDeepLinkRejectsNonDocumentCollection() throws {
        let url = try XCTUnwrap(
            URL(string: "https://inkwell.ewancroft.uk/open?uri=at://did:plc:abc123/site.standard.publication/xyz")
        )
        XCTAssertNil(HttpsDeepLinkPolicy.candidateDocumentURI(from: url))
    }

    func testHttpsDeepLinkRejectsMissingURIParameter() throws {
        let url = try XCTUnwrap(URL(string: "https://inkwell.ewancroft.uk/open?url=https://example.com/article"))
        XCTAssertNil(HttpsDeepLinkPolicy.candidateDocumentURI(from: url))
    }
}
