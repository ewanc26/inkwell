import XCTest
@testable import Inkwell

final class NotificationMetadataTests: XCTestCase {
    func testVisibleMetadataPreservesTitleAndPublication() {
        let notification = notificationMetadata(
            documentURI: "at://did:plc:author/site.standard.document/post",
            title: "Visible title",
            publicationName: "A publication",
            publishedAt: Date(timeIntervalSince1970: 1),
            sensitive: false,
            date: Date(timeIntervalSince1970: 2)
        )

        XCTAssertEqual(notification.documentTitle, "Visible title")
        XCTAssertEqual(notification.publicationName, "A publication")
    }

    func testSensitiveMetadataRedactsTitleAndPublication() {
        let notification = notificationMetadata(
            documentURI: "at://did:plc:author/site.standard.document/hidden",
            title: "Sensitive title",
            publicationName: "Sensitive publication",
            publishedAt: Date(timeIntervalSince1970: 1),
            sensitive: true,
            date: Date(timeIntervalSince1970: 2)
        )

        XCTAssertEqual(notification.documentTitle, "Hidden document")
        XCTAssertNil(notification.publicationName)
    }
}
