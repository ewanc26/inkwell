import XCTest
@testable import Inkwell

final class BidiTextTests: XCTestCase {
    func testIsolatesUserTextWithFirstStrongIsolateControls() {
        XCTAssertEqual("שלום".bidiIsolated, "\u{2068}שלום\u{2069}")
    }

    func testPreservesEmptyUserText() {
        XCTAssertEqual("".bidiIsolated, "\u{2068}\u{2069}")
    }
}
