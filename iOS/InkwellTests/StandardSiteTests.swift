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
import ImageIO
import XCTest
import UniformTypeIdentifiers
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
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://pds.example/path",
            "https://pds.example"
        ))
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://pds.example?issuer=other#fragment",
            "https://pds.example"
        ))
        XCTAssertFalse(OAuthIssuerPolicy.sameHTTPSOrigin(
            "https://pds.example:8443",
            "HTTPS://PDS.EXAMPLE:9443/"
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

    func testReadingDataImportRejectsOversizedTitleAndFutureTimestamp() {
        let oversizedTitle = String(repeating: "x", count: 501)
        let oversized = Data(("{\"format\":\"uk.ewancroft.inkwell.reading-data\",\"version\":1,\"exportedAt\":\"2026-09-19T00:00:00Z\",\"articles\":[{\"articleId\":\"at://did:plc:alice/site.standard.document/one\",\"title\":\"" + oversizedTitle + "\",\"isRead\":false,\"isBookmarked\":false,\"timestamp\":\"2026-09-19T00:00:00Z\"}]}" ).utf8)
        let future = Data(#"{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"at://did:plc:alice/site.standard.document/one","title":"ok","isRead":false,"isBookmarked":false,"timestamp":"2999-01-01T00:00:00Z"}]}"#.utf8)

        XCTAssertThrowsError(try ArticleStateStore.shared.previewImportJSON(oversized))
        XCTAssertThrowsError(try ArticleStateStore.shared.previewImportJSON(future))
    }

    func testReadingDataImportRejectsFutureExportTimestamp() {
        let data = Data(#"{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2999-01-01T00:00:00Z","articles":[]}"#.utf8)
        XCTAssertThrowsError(try ArticleStateStore.shared.previewImportJSON(data))
    }

    func testReadingDataImportPreviewDeduplicatesByNewestTimestamp() throws {
        let data = Data(#"{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"at://did:plc:dedupe/site.standard.document/one","title":"old","isRead":false,"isBookmarked":false,"timestamp":"2026-09-18T00:00:00Z"},{"articleId":"at://did:plc:dedupe/site.standard.document/one","title":"new","isRead":true,"isBookmarked":true,"timestamp":"2026-09-19T00:00:00Z"}]}"#.utf8)

        XCTAssertEqual(try ArticleStateStore.shared.previewImportJSON(data), 1)
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

    func testImageSanitizerDerivesMimeFromEncodedOutput() throws {
        let onePixelPNG = Data(base64Encoded: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=")!

        let output = try ImageUploadSanitizer.sanitize(onePixelPNG)

        XCTAssertEqual(output.mimeType, "image/png")
        XCTAssertEqual(Array(output.data.prefix(8)), [137, 80, 78, 71, 13, 10, 26, 10])
    }

    func testImageSanitizerRemovesLocationMetadataAndNormalizesOrientation() throws {
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        let context = CGContext(
            data: nil,
            width: 2,
            height: 3,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: colorSpace,
            bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
        )!
        context.setFillColor(CGColor(red: 1, green: 0, blue: 0, alpha: 1))
        context.fill(CGRect(x: 0, y: 0, width: 2, height: 3))
        let image = context.makeImage()!
        let input = NSMutableData()
        let destination = CGImageDestinationCreateWithData(input, UTType.jpeg.identifier as CFString, 1, nil)!
        CGImageDestinationAddImage(destination, image, [
            kCGImagePropertyOrientation: 6,
            kCGImagePropertyGPSDictionary: [
                kCGImagePropertyGPSLatitude: 51.5,
                kCGImagePropertyGPSLongitude: -0.1,
            ],
        ] as CFDictionary)
        XCTAssertTrue(CGImageDestinationFinalize(destination))

        let output = try ImageUploadSanitizer.sanitize(input as Data)
        let source = CGImageSourceCreateWithData(output.data as CFData, nil)!
        let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any]

        XCTAssertNil(properties?[kCGImagePropertyGPSDictionary])
        XCTAssertEqual(properties?[kCGImagePropertyOrientation] as? Int, 1)
    }

    func testImageSanitizerRejectsAnimatedGIF() {
        let animatedGIF = Data(base64Encoded: "R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")!

        XCTAssertThrowsError(try ImageUploadSanitizer.sanitize(animatedGIF)) { error in
            guard case .animatedImage = error as? ImageUploadSanitizer.Failure else {
                return XCTFail("Expected animated-image rejection, got \(error)")
            }
        }
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

    func testIframeSecurityPolicyRequiresSameHTTPSOrigin() {
        let origin = URL(string: "https://embed.example:8443/frame")!
        XCTAssertTrue(IframeSecurityPolicy.isAllowedInitial(origin))
        XCTAssertTrue(IframeSecurityPolicy.isAllowedNavigation(from: origin, to: URL(string: "https://EMBED.example:8443/next")!))
        XCTAssertFalse(IframeSecurityPolicy.isAllowedNavigation(from: origin, to: URL(string: "https://embed.example/next")!))
        XCTAssertFalse(IframeSecurityPolicy.isAllowedNavigation(from: origin, to: URL(string: "https://other.example:8443/next")!))
        XCTAssertFalse(IframeSecurityPolicy.isAllowedInitial(URL(string: "http://embed.example/frame")!))
        XCTAssertFalse(IframeSecurityPolicy.isAllowedInitial(URL(string: "javascript:alert(1)")!))
    }

    func testRetryAfterPolicyParsesDeltaHTTPDateAndFallback() {
        let now = Date(timeIntervalSince1970: 1_800_000_000)
        let httpDate = ISO8601DateFormatter().date(from: "2026-09-20T00:00:05Z")!
        XCTAssertEqual(RetryAfterPolicy.delay(for: "5", attempt: 0, now: now), 5, accuracy: 0.001)
        XCTAssertEqual(
            RetryAfterPolicy.delay(for: "Sun, 20 Sep 2026 00:00:05 GMT", attempt: 0, now: httpDate.addingTimeInterval(-5)),
            5,
            accuracy: 0.001
        )
        XCTAssertEqual(RetryAfterPolicy.delay(for: "invalid", attempt: 2, now: now), 0.4, accuracy: 0.001)
        XCTAssertEqual(RetryAfterPolicy.delay(for: "999", attempt: 0, now: now), 60, accuracy: 0.001)
    }

    private func document(
        site: String,
        path: String? = nil
    ) -> SiteStandardLexicon.DocumentRecord {
        .init(site: site, title: "Hello", publishedAt: Date(timeIntervalSince1970: 0), path: path)
    }
}
