import XCTest
@testable import Inkwell

final class BlobDownloadTests: XCTestCase {
    func testBlobUploadRequestUsesRawBytesAndActualMimeType() throws {
        let data = Data([0x00, 0x01, 0xfe, 0xff])
        let request = makeBlobUploadRequest(
            url: URL(string: "https://pds.example/xrpc/com.atproto.repo.uploadBlob")!,
            data: data,
            mimeType: "image/png"
        )

        XCTAssertEqual(request.httpMethod, "POST")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Type"), "image/png")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Content-Length"), "4")
        XCTAssertEqual(request.value(forHTTPHeaderField: "Accept"), "application/json")
        XCTAssertEqual(request.httpBody, data)
        XCTAssertNil(request.value(forHTTPHeaderField: "Content-Disposition"))
    }

    func testDeclaredBlobSizeAtLimitIsAccepted() throws {
        XCTAssertNoThrow(try validateDeclaredBlobSize(10 * 1024 * 1024))
    }

    func testDeclaredBlobSizeOverLimitIsRejected() {
        XCTAssertThrowsError(try validateDeclaredBlobSize(10 * 1024 * 1024 + 1))
    }

    func testNegativeDeclaredBlobSizeIsRejected() {
        XCTAssertThrowsError(try validateDeclaredBlobSize(-1))
    }
}
