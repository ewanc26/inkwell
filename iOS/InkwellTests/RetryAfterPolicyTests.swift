import XCTest
@testable import Inkwell

final class RetryAfterPolicyTests: XCTestCase {
    func testParsesAndCapsDeltaSeconds() {
        XCTAssertEqual(RetryAfterPolicy.delay(for: "5", attempt: 0), 5, accuracy: 0.001)
        XCTAssertEqual(RetryAfterPolicy.delay(for: "999", attempt: 0), RetryAfterPolicy.maxDelay, accuracy: 0.001)
    }

    func testParsesHTTPDateAgainstInjectedClock() {
        let now = Date(timeIntervalSince1970: 1_789_862_400)
        XCTAssertEqual(
            RetryAfterPolicy.delay(for: "Tue, 20 Sep 2026 00:00:05 GMT", attempt: 0, now: now),
            5,
            accuracy: 0.001
        )
    }

    func testMalformedHeaderUsesBoundedBackoff() {
        XCTAssertEqual(RetryAfterPolicy.delay(for: "not-a-date", attempt: 0), 0.1, accuracy: 0.001)
        XCTAssertEqual(RetryAfterPolicy.delay(for: "not-a-date", attempt: 2), 0.4, accuracy: 0.001)
        XCTAssertEqual(RetryAfterPolicy.delay(for: "not-a-date", attempt: 20), RetryAfterPolicy.maxDelay, accuracy: 0.001)
    }

    func testOriginUsesSchemeDefaultPortAndNormalizesHost() {
        XCTAssertEqual(
            RetryAfterPolicy.origin(for: URL(string: "HTTP://PDS.Example")!),
            "http://pds.example:80"
        )
        XCTAssertEqual(
            RetryAfterPolicy.origin(for: URL(string: "https://PDS.Example")!),
            "https://pds.example:443"
        )
        XCTAssertEqual(
            RetryAfterPolicy.origin(for: URL(string: "https://PDS.Example:8443")!),
            "https://pds.example:8443"
        )
    }
}
