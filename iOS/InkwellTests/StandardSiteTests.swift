//
//  StandardSiteTests.swift
//  Inkwell
//
//  Unit tests for the AT-URI parser, publication/document association,
//  canonical URL construction, verification endpoints, wire-format
//  encoding, search response decoding, notification persistence, and
//  tolerant record-page decoding that drops malformed records instead
//  of failing the entire batch.
//

import Foundation
import XCTest
@testable import Inkwell

// MARK: - Standard.site Tests

@MainActor
final class StandardSiteTests: XCTestCase {
    func testInkwellNSIDNamespace() {
        XCTAssertEqual(InkwellIdentifiers.lexiconNamespace, "uk.ewancroft.inkwell")
        XCTAssertEqual(BackgroundRefreshManager.taskIdentifier, "uk.ewancroft.inkwell.refresh")
    }

    func testATURIParsingRejectsMalformedValues() {
        XCTAssertEqual(
            parseAtUri("at://did:plc:alice/site.standard.document/3abc")?.recordKey,
            "3abc"
        )
        XCTAssertNil(parseAtUri("https://example.com/post"))
        XCTAssertNil(parseAtUri("at://did:plc:alice/site.standard.document"))
    }

    func testOAuthIssuerPolicyRequiresNormalizedHTTPSOriginEquality() {
        XCTAssertTrue(OAuthIssuerPolicy.sameHTTPSOrigin(
            "HTTPS://PDS.EXAMPLE:443",
            "https://pds.example"
        ))
        XCTAssertTrue(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://pds.example:8443",
            "https://PDS.EXAMPLE:8443/"
        ))
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://evil.example/pds.example",
            "https://pds.example"
        ))
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://user:p@pds.example",
            "https://pds.example"
        ))
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "http://pds.example",
            "https://pds.example"
        ))
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

        XCTAssertTrue(sharedDocumentBelongsToPublication(
            documentSite: atDocument.site,
            publicationUri: publication.uri,
            publicationUrl: publication.record.url
        ))
        XCTAssertTrue(sharedDocumentBelongsToPublication(
            documentSite: urlDocument.site,
            publicationUri: publication.uri,
            publicationUrl: publication.record.url
        ))
        XCTAssertFalse(sharedDocumentBelongsToPublication(
            documentSite: otherDocument.site,
            publicationUri: publication.uri,
            publicationUrl: publication.record.url
        ))
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
            ),
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
        // The pub search backend sends total: null and camelCase field names.
        let data = Data(#"{"results":[{"type":"article","uri":"at://did:plc:alice/site.standard.document/3doc","did":"did:plc:alice","title":"Hello","platform":"pckt","path":"/hello","basePath":"alice.pckt.blog","createdAt":"2026-06-01T12:00:00Z"}],"total":null,"hasMore":false}"#.utf8)
        let response = try JSONDecoder().decode(ReaderSearchResponse.self, from: data)

        XCTAssertNil(response.total)
        XCTAssertEqual(response.results.first?.webURL?.absoluteString, "https://alice.pckt.blog/hello")
        XCTAssertTrue(response.results.first?.isStandardSiteDocument == true)
    }

    func testSearchV2PublicationResultKeepsATURIIdentity() throws {
        let data = Data(#"{"results":[{"type":"publication","uri":"at://did:plc:alice/site.standard.publication/blog","did":"did:plc:alice","title":"My Blog"}],"total":null,"hasMore":false}"#.utf8)
        let response = try JSONDecoder().decode(ReaderSearchResponse.self, from: data)
        let result = try XCTUnwrap(response.results.first)

        XCTAssertTrue(result.isPublication)
        XCTAssertEqual(result.uri, "at://did:plc:alice/site.standard.publication/blog")
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

    func testRecordPageSkipsMalformedRecordsAndAllowsMissingCID() throws {
        let data = Data(#"""
        {
          "cursor": "next",
          "records": [
            {
              "uri": "at://did:plc:alice/site.standard.document/valid",
              "value": {
                "$type": "site.standard.document",
                "site": "https://example.com",
                "title": "Valid",
                "publishedAt": "2026-06-20T12:00:00Z"
              }
            },
            {
              "cid": "bafymalformed",
              "value": { "$type": "site.standard.document" }
            }
          ]
        }
        """#.utf8)

        let page = try JSONDecoder().decode(TolerantRecordPage.self, from: data)

        XCTAssertEqual(page.cursor, "next")
        XCTAssertEqual(page.records.count, 1)
        XCTAssertNil(page.records.first?.cid)
        XCTAssertEqual(page.records.first?.uri, "at://did:plc:alice/site.standard.document/valid")
    }

    func testReadingDataImportPreviewAcceptsVersionOneEnvelope() throws {
        let data = Data(#"""
        {"format":"uk.ewancroft.inkwell.reading-data","version":1,
         "exportedAt":"2026-09-19T00:00:00Z","articles":[
           {"articleId":"at://did:plc:alice/site.standard.document/one",
            "title":"Example","isRead":true,"isBookmarked":false,
            "timestamp":"2026-09-19T00:00:00Z","futureField":"ignored"}
         ]}
        """#.utf8)

        XCTAssertGreaterThanOrEqual(try ArticleStateStore.shared.previewImportJSON(data), 0)
    }

    func testReadingDataImportRejectsUnsupportedVersionAndMalformedArticle() {
        let unsupported = Data(#"{"format":"uk.ewancroft.inkwell.reading-data","version":2,"exportedAt":"2026-09-19T00:00:00Z","articles":[]}"#.utf8)
        XCTAssertThrowsError(try ArticleStateStore.shared.previewImportJSON(unsupported))

        let malformed = Data(#"{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"https://example.com","title":"bad","isRead":false,"isBookmarked":false,"timestamp":"2026-09-19T00:00:00Z"}]}"#.utf8)
        XCTAssertThrowsError(try ArticleStateStore.shared.previewImportJSON(malformed))
    }

    func testNotificationNavigationConsumesAQueuedDocumentExactlyOnce() {
        let coordinator = NotificationNavigationCoordinator.shared
        coordinator.enqueue(documentURI: "at://did:plc:alice/site.standard.document/queued")

        XCTAssertEqual(
            coordinator.consumePendingDocumentURI(),
            "at://did:plc:alice/site.standard.document/queued"
        )
        XCTAssertNil(coordinator.consumePendingDocumentURI())
    }

    func testNotificationNavigationReplacesStalePendingRoute() {
        let coordinator = NotificationNavigationCoordinator.shared
        coordinator.enqueue(documentURI: "at://did:plc:alice/site.standard.document/old")
        coordinator.enqueue(documentURI: "at://did:plc:alice/site.standard.document/new")

        XCTAssertEqual(
            coordinator.consumePendingDocumentURI(),
            "at://did:plc:alice/site.standard.document/new"
        )
        XCTAssertNil(coordinator.consumePendingDocumentURI())
    }

    func testImageSanitizerRejectsOversizedEncodedInputBeforeDecoding() {
        let oversized = Data(repeating: 0, count: 10 * 1024 * 1024 + 1)
        XCTAssertThrowsError(try ImageUploadSanitizer.sanitize(oversized))
    }

    func testContentDeepLinkPolicyAcceptsEncodedDocumentURI() {
        let uri = "at://did:plc:alice/site.standard.document/café"
        let encoded = uri.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)!
        let url = URL(string: "inkwell://document?uri=\(encoded)")!

        XCTAssertEqual(ContentDeepLinkPolicy.documentURI(from: url), uri)
    }

    func testContentDeepLinkPolicyRejectsPublicationAndOAuthLinks() {
        let publication = URL(string: "inkwell://document?uri=at://did:plc:alice/site.standard.publication/pub")!
        let oauth = URL(string: "uk.ewancroft.inkwell:/callback?uri=at://did:plc:alice/site.standard.document/doc")!

        XCTAssertNil(ContentDeepLinkPolicy.documentURI(from: publication))
        XCTAssertNil(ContentDeepLinkPolicy.documentURI(from: oauth))
    }

    private func document(
        site: String,
        path: String? = nil
    ) -> SiteStandardLexicon.DocumentRecord {
        .init(site: site, title: "Hello", publishedAt: Date(timeIntervalSince1970: 0), path: path)
    }
}
