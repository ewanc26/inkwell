import XCTest
@testable import Inkwell

final class WriterRecordSizeTests: XCTestCase {
    func testRecordAtLimitIsAccepted() throws {
        XCTAssertNoThrow(try validateDocumentRecordSize(900 * 1024))
    }

    func testRecordOverLimitIsRejected() {
        XCTAssertThrowsError(try validateDocumentRecordSize(900 * 1024 + 1))
    }
}
