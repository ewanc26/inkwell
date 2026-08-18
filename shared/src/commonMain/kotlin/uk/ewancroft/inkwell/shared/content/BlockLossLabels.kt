package uk.ewancroft.inkwell.shared.content

/**
 * Shared loss label maps per content format.
 *
 * Maps block type strings that can't be represented as markdown to
 * human-readable labels shown to the user. Centralises the labels
 * currently duplicated across iOS ContentProvider.swift and Android
 * PcktOffprintConverter.kt / MarkdownConverter.kt.
 */
object BlockLossLabels {

    val leaflet: Map<String, String> = mapOf(
        LeafletTypes.BLOCKS_IFRAME to "embeds",
        LeafletTypes.BLOCKS_WEBSITE to "website cards",
        LeafletTypes.BLOCKS_BSKY_POST to "Bluesky posts",
        LeafletTypes.BLOCKS_STANDARD_SITE_POST to "linked posts",
        LeafletTypes.BLOCKS_PAGE to "sub-pages",
        LeafletTypes.BLOCKS_POLL to "polls",
        LeafletTypes.BLOCKS_BUTTON to "buttons",
        LeafletTypes.BLOCKS_POSTS_LIST to "post lists",
        LeafletTypes.BLOCKS_SIGNUP to "signup forms",
    )

    val pckt: Map<String, String> = mapOf(
        PcktTypes.BLOCK_TABLE to "tables",
        PcktTypes.BLOCK_MENTION to "mention blocks",
        PcktTypes.BLOCK_GALLERY to "galleries",
        PcktTypes.BLOCK_IFRAME to "embeds",
        PcktTypes.BLOCK_WEBSITE to "website cards",
        PcktTypes.BLOCK_BLUESKY_EMBED to "Bluesky posts",
        PcktTypes.BLOCK_NOTE_EMBED to "note embeds",
        PcktTypes.BLOCK_HARD_BREAK to "hard breaks",
    )

    val offprint: Map<String, String> = mapOf(
        OffprintTypes.BLOCK_CALLOUT to "callouts",
        OffprintTypes.BLOCK_BUTTON to "buttons",
        OffprintTypes.BLOCK_WEB_BOOKMARK to "bookmarks",
        OffprintTypes.BLOCK_WEB_EMBED to "embeds",
        OffprintTypes.BLOCK_BLUESKY_POST to "Bluesky posts",
        OffprintTypes.BLOCK_IMAGE_GRID to "image grids",
        OffprintTypes.BLOCK_IMAGE_CAROUSEL to "image carousels",
        OffprintTypes.BLOCK_IMAGE_DIFF to "image comparisons",
    )
}
