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

    func testNotificationUnreadStateIsIsolatedAcrossAccountSwitches() {
        let defaults = UserDefaults.standard
        let accountAKey = "standardSite.account.did:plc:account-a.unreadCount"
        let accountBKey = "standardSite.account.did:plc:account-b.unreadCount"
        defaults.set(7, forKey: accountAKey)
        defaults.set(2, forKey: accountBKey)
        defer {
            defaults.removeObject(forKey: accountAKey)
            defaults.removeObject(forKey: accountBKey)
            NotificationManager.shared.deactivate()
        }

        let manager = NotificationManager.shared
        manager.activate(accountDID: "did:plc:account-a")
        XCTAssertEqual(manager.unreadCount, 7)
        manager.activate(accountDID: "did:plc:account-b")
        XCTAssertEqual(manager.unreadCount, 2)
        manager.activate(accountDID: "did:plc:account-a")
        XCTAssertEqual(manager.unreadCount, 7)
    }
}
