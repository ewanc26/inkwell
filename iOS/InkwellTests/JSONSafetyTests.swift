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

    func testRejectsMultibyteStringOverUtf8ByteBudget() {
        let characterCount = JSONSafety.maxStringBytes / 2 + 1
        let json = "{\"value\":\"" + String(repeating: "é", count: characterCount) + "\"}"
        XCTAssertThrowsError(try JSONSafety.validate(Data(json.utf8)))
    }

    func testRejectsOversizedResponseBeforeStructuralValidation() {
        let data = Data(repeating: 0x20, count: JSONSafety.maxResponseBytes + 1)
        XCTAssertThrowsError(try JSONSafety.validateResponse(data))
    }

    func testRejectsTooManyContainerElements() throws {
        let values = Array(repeating: "true", count: JSONSafety.maxContainerElements + 1)
        let json = "[" + values.joined(separator: ",") + "]"
        XCTAssertThrowsError(try JSONSafety.validate(Data(json.utf8)))
    }
}
