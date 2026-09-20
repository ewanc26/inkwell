import XCTest
@testable import Inkwell

@MainActor
final class NotificationNavigationCoordinatorTests: XCTestCase {
    func testPendingDocumentIsConsumedExactlyOnce() {
        let coordinator = NotificationNavigationCoordinator()

        coordinator.enqueue(documentURI: "at://did:plc:test/site.standard.document/post")

        XCTAssertEqual(
            coordinator.pendingDocumentURI,
            "at://did:plc:test/site.standard.document/post"
        )
        XCTAssertEqual(
            coordinator.consumePendingDocumentURI(),
            "at://did:plc:test/site.standard.document/post"
        )
        XCTAssertNil(coordinator.pendingDocumentURI)
        XCTAssertNil(coordinator.consumePendingDocumentURI())
    }

    func testLaterNotificationReplacesStalePendingRoute() {
        let coordinator = NotificationNavigationCoordinator()

        coordinator.enqueue(documentURI: "at://did:plc:test/site.standard.document/old")
        coordinator.enqueue(documentURI: "at://did:plc:test/site.standard.document/new")

        XCTAssertEqual(
            coordinator.consumePendingDocumentURI(),
            "at://did:plc:test/site.standard.document/new"
        )
    }
}
