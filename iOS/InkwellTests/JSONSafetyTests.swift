import XCTest
@testable import Inkwell

final class JSONSafetyTests: XCTestCase {
    func testAcceptsNormalJSON() throws {
        XCTAssertNoThrow(try JSONSafety.validate(Data(#"{"title":"hello"}"#.utf8)))
    }

    func testRejectsExcessiveNesting() {
        var json = "{}"
        for _ in 0...(JSONSafety.maxDepth) { json = #"{"child":"# + json + #"}"# }
        XCTAssertThrowsError(try JSONSafety.validate(Data(json.utf8)))
    }

    func testRejectsOversizedString() {
        let json = "{\"value\":\"" + String(repeating: "x", count: JSONSafety.maxStringBytes + 1) + "\"}"
        XCTAssertThrowsError(try JSONSafety.validate(Data(json.utf8)))
    }
}
