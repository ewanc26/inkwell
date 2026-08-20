package uk.ewancroft.inkwell.shared.support

/**
 * Identifies Inkwell's Bluesky supporters list — people who've tipped via
 * Ko-fi or GitHub Sponsors, curated manually by ewancroft.uk. Not bridged
 * through the XCFramework (see [uk.ewancroft.inkwell.shared.feedback.UserInputLexicon]
 * for the same convention); iOS keeps its own literal copy in
 * `BSkyListFetcher.swift`.
 */
object SupportersList {
    /** at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27 */
    const val URI = "at://did:plc:ofrbh253gwicbkc5nktqepol/app.bsky.graph.list/3mtjkyzm3nx27"
}
