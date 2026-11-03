import XCTest
@testable import Inkwell

/// Leaflet's lexicons store `aspectRatio` as `{ width, height }`; records from
/// before that shape settled used a string such as `16:9`. Both must decode.
@MainActor
final class LeafletAspectRatioTests: XCTestCase {

    func testDecodesCanonicalAspectRatioObject() throws {
        let json = """
        {"$type":"pub.leaflet.blocks.iframe","url":"https://example.com/embed","height":600,"aspectRatio":{"width":16,"height":9}}
        """
        let block = try JSONDecoder().decode(LeafletBlock.self, from: Data(json.utf8))

        XCTAssertEqual(block.aspectRatio?.width, 16)
        XCTAssertEqual(block.aspectRatio?.height, 9)
        XCTAssertEqual(block.height, 600)
    }

    func testDecodesLegacyAspectRatioString() throws {
        let json = """
        {"$type":"pub.leaflet.blocks.iframe","url":"https://example.com/embed","aspectRatio":"4:3"}
        """
        let block = try JSONDecoder().decode(LeafletBlock.self, from: Data(json.utf8))

        XCTAssertEqual(block.aspectRatio?.width, 4)
        XCTAssertEqual(block.aspectRatio?.height, 3)
    }

    func testDecodesImageGallery() throws {
        let json = """
        {"$type":"pub.leaflet.blocks.imageGallery","format":"strip","gap":12,"images":[{"$type":"pub.leaflet.blocks.imageGallery#image","image":{"$type":"blob","ref":{"$link":"bafyone"},"mimeType":"image/jpeg","size":1},"alt":"One","aspectRatio":{"width":4,"height":3}}]}
        """
        let block = try JSONDecoder().decode(LeafletBlock.self, from: Data(json.utf8))

        XCTAssertEqual(block.galleryFormat, "strip")
        XCTAssertEqual(block.galleryGap, 12)
        XCTAssertEqual(block.images?.count, 1)
        XCTAssertEqual(block.images?.first?.aspectRatio?.width, 4)
        XCTAssertEqual(block.images?.first?.aspectRatio?.height, 3)
    }
}
