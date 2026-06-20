import Foundation
import XCTest
@testable import Inkwell

@MainActor
final class StandardSiteTests: XCTestCase {
    func testInkwellNSIDNamespace() {
        XCTAssertEqual(InkwellIdentifiers.lexiconNamespace, "uk.ewancroft.inkwell")
        XCTAssertEqual(BackgroundRefreshManager.taskIdentifier, "uk.ewancroft.inkwell.refresh")
    }

    func testATURIParsingRejectsMalformedValues() {
        XCTAssertEqual(
            ATURI.parse("at://did:plc:alice/site.standard.document/3abc")?.recordKey,
            "3abc"
        )
        XCTAssertNil(ATURI.parse("https://example.com/post"))
        XCTAssertNil(ATURI.parse("at://did:plc:alice/site.standard.document"))
    }

    func testPublicationAssociationPrefersATURIAndAcceptsNormalizedURL() {
        let publication = PublicationEntry(
            uri: "at://did:plc:alice/site.standard.publication/3pub",
            authorDID: "did:plc:alice",
            record: .init(url: "https://example.com/blog/", name: "Example")
        )
        let atDocument = document(site: publication.uri)
        let urlDocument = document(site: "https://example.com/blog")
        let otherDocument = document(site: "https://elsewhere.example")

        XCTAssertTrue(publication.contains(atDocument))
        XCTAssertTrue(publication.contains(urlDocument))
        XCTAssertFalse(publication.contains(otherDocument))
    }

    func testCanonicalURLUsesPublicationURLForATURISite() {
        let publication = SiteStandardLexicon.PublicationRecord(
            url: "https://example.com/writing",
            name: "Example"
        )
        let value = document(
            site: "at://did:plc:alice/site.standard.publication/3pub",
            path: "/posts/hello"
        )

        XCTAssertEqual(
            value.canonicalURL(publication: publication)?.absoluteString,
            "https://example.com/writing/posts/hello"
        )
        XCTAssertNil(value.canonicalURL())
    }

    func testNonRootPublicationVerificationEndpoint() {
        XCTAssertEqual(
            SiteStandardLexicon.Verification.publicationVerificationURL(
                for: "https://example.com/writing/"
            )?.absoluteString,
            "https://example.com/.well-known/site.standard.publication/writing"
        )
    }

    func testDocumentEncodingUsesStandardSiteWireKeys() throws {
        let value = document(site: "https://example.com", path: "/hello")
        let data = try JSONEncoder().encode(value)
        let json = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])

        XCTAssertEqual(json["$type"] as? String, "site.standard.document")
        XCTAssertEqual(json["site"] as? String, "https://example.com")
        XCTAssertEqual(json["path"] as? String, "/hello")
        XCTAssertEqual(json["title"] as? String, "Hello")
    }

    func testSearchV2ResponseDecoding() throws {
        let data = Data(#"{"results":[{"type":"article","uri":"at://did:plc:alice/site.standard.document/3doc","did":"did:plc:alice","title":"Hello","platform":"pckt","path":"/hello","basePath":"alice.pckt.blog"}],"total":1,"hasMore":false}"#.utf8)
        let response = try JSONDecoder().decode(ReaderSearchResponse.self, from: data)

        XCTAssertEqual(response.results.first?.webURL?.absoluteString, "https://alice.pckt.blog/hello")
        XCTAssertTrue(response.results.first?.isStandardSiteDocument == true)
    }

    func testNotificationRoundTripsThroughJSON() throws {
        let notification = StandardSiteNotification(
            documentURI: "at://did:plc:alice/site.standard.document/3doc",
            documentTitle: "Hello",
            publicationName: "Example",
            publishedAt: Date(timeIntervalSince1970: 100),
            date: Date(timeIntervalSince1970: 200)
        )
        let decoded = try JSONDecoder().decode(
            StandardSiteNotification.self,
            from: JSONEncoder().encode(notification)
        )
        XCTAssertEqual(decoded, notification)
    }

    private func document(
        site: String,
        path: String? = nil
    ) -> SiteStandardLexicon.DocumentRecord {
        .init(site: site, title: "Hello", publishedAt: Date(timeIntervalSince1970: 0), path: path)
    }
}
