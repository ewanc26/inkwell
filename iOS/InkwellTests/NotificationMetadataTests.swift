import XCTest
import InkwellShared
@testable import Inkwell

@MainActor
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

    func testPublicationLevelLabelIsRedactedBySharedPolicy() {
        let settings = ModerationSettings.shared
        let oldHidden = settings.hiddenLabels
        let oldWarnings = settings.warningLabels
        defer {
            settings.hiddenLabels = oldHidden
            settings.warningLabels = oldWarnings
        }

        settings.hiddenLabels = ["adult"]
        XCTAssertTrue(shouldRedactNotification(
            title: "Sensitive title",
            description: nil,
            textContent: nil,
            labels: [ModerationLabel(value: "adult", source: "publication")],
            settings: settings
        ))
    }

    func testWarningLabelIsRedactedBeforeReaderReveal() {
        let settings = ModerationSettings.shared
        let oldHidden = settings.hiddenLabels
        let oldWarnings = settings.warningLabels
        defer {
            settings.hiddenLabels = oldHidden
            settings.warningLabels = oldWarnings
        }

        settings.warningLabels = ["spoiler"]
        XCTAssertTrue(shouldRedactNotification(
            title: "Spoiler title",
            description: "Spoiler description",
            textContent: nil,
            labels: [ModerationLabel(value: "spoiler", source: "document")],
            settings: settings
        ))
    }
}
