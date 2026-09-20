import XCTest
@testable import Inkwell

final class BlobDownloadTests: XCTestCase {
    func testDeclaredBlobSizeAtLimitIsAccepted() throws {
        XCTAssertNoThrow(try validateDeclaredBlobSize(10 * 1024 * 1024))
    }

    func testDeclaredBlobSizeOverLimitIsRejected() {
        XCTAssertThrowsError(try validateDeclaredBlobSize(10 * 1024 * 1024 + 1))
    }
}
