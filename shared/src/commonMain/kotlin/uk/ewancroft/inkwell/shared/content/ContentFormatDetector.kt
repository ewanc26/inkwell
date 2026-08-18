package uk.ewancroft.inkwell.shared.content

/**
 * Shared content format type detection.
 *
 * Maps AT Protocol record `$type` strings to known content formats.
 * Both platforms use these constants to dispatch rendering.
 */
object ContentFormatDetector {

    const val LEAFLET = "pub.leaflet.content"
    const val MARKPUB = "at.markpub.markdown"
    const val PCKT = "blog.pckt.content"
    const val OFFPRINT = "app.offprint.content"

    /**
     * All known content format type strings.
     */
    val ALL: List<String> = listOf(LEAFLET, MARKPUB, PCKT, OFFPRINT)

    /**
     * Returns true if [type] is a recognised content format.
     */
    fun isKnown(type: String?): Boolean =
        type != null && type in ALL

    /**
     * Returns true if [type] is a pckt or Offprint format
     * (both use the same block-array converter).
     */
    fun isPcktOrOffprint(type: String?): Boolean =
        type == PCKT || type == OFFPRINT
}
