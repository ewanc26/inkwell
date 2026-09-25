import Foundation
import XCTest
import ATProtoKit
@testable import Inkwell

@MainActor
final class PollModelsTests: XCTestCase {
    func testDecodesLeafletPollDefinitionWireShape() throws {
        let json = """
        {
            "$type": "pub.leaflet.poll.definition",
            "name": "Which format do you prefer?",
            "options": [
                {"text": "Leaflet"},
                {"text": "Markpub"}
            ]
        }
        """

        let decoder = JSONDecoder()
        let definition = try decoder.decode(LeafletPollDefinition.self, from: Data(json.utf8))

        XCTAssertEqual(definition.name, "Which format do you prefer?")
        XCTAssertEqual(definition.options.count, 2)
        XCTAssertEqual(definition.options[0].text, "Leaflet")
        XCTAssertEqual(definition.options[1].text, "Markpub")
    }

    func testDecodesLeafletPollVoteWireShape() throws {
        let json = """
        {
            "$type": "pub.leaflet.poll.vote",
            "poll": {
                "uri": "at://did:plc:author/pub.leaflet.poll.definition/3k2",
                "cid": "bafybeigdyrzt5sfp7oi4y0"
            },
            "option": ["Leaflet"]
        }
        """

        let decoder = JSONDecoder()
        let vote = try decoder.decode(LeafletPollVote.self, from: Data(json.utf8))

        XCTAssertEqual(vote.poll.recordURI, "at://did:plc:author/pub.leaflet.poll.definition/3k2")
        XCTAssertEqual(vote.option, ["Leaflet"])
    }
}
