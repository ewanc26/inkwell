import XCTest
@testable import Inkwell

final class WriterDeletePolicyTests: XCTestCase {
    func testStaleRevisionProducesReloadConflict() {
        XCTAssertEqual(
            writerDeleteErrorMessage(NSError(domain: "ATProto", code: 409, userInfo: [NSLocalizedDescriptionKey: "InvalidSwap"])),
            "This document changed elsewhere. Reload it before deleting."
        )
    }

    func testCancellationPreservesGenericFailureMessage() {
        XCTAssertEqual(
            writerDeleteErrorMessage(NSError(domain: NSCocoaErrorDomain, code: NSUserCancelledError)),
            "Failed to delete document: The operation was cancelled."
        )
    }

    func testInvalidSwapDuringEditRequestsReload() {
        let error = NSError(
            domain: "ATProto",
            code: 409,
            userInfo: [NSLocalizedDescriptionKey: "InvalidSwap"]
        )

        XCTAssertEqual(
            writerEditErrorMessage(error),
            "This document changed elsewhere. Reload it before saving."
        )
    }
}
