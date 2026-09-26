package uk.ewancroft.inkwell.data.model.bluesky

/**
 * Pure formatting/extraction logic for [BSkyExternal] embeds, kept separate
 * from decoding (and free of any Compose/Android dependency) so it's
 * unit-testable and reusable by both the block renderer and any future
 * consumer of enriched Standard.site embed metadata.
 */

/**
 * Whether this card carries Standard.site-enriched metadata (a source,
 * reading time, or associated content) rather than being a plain Bluesky
 * external-link card. Plain cards must keep rendering unchanged, so all
 * enriched-only UI is gated on this.
 */
fun BSkyExternal.isStandardSiteEnriched(): Boolean =
    source != null || readingTime != null || !associatedRefs.isNullOrEmpty()

/** "5 min read", or `null` when no positive reading time is available. */
fun BSkyExternal.readingTimeLabel(): String? {
    val minutes = readingTime ?: return null
    if (minutes <= 0) return null
    return "$minutes min read"
}

/**
 * Non-empty label values, suitable for a content-warning row. Negation
 * labels and blank values are excluded since they don't represent an
 * active warning to display.
 */
fun BSkyExternal.contentWarningLabels(): List<String> =
    labels?.mapNotNull { label ->
        val v = label.value
        if (label.neg == true || v.isNullOrBlank()) null else v
    } ?: emptyList()

/**
 * The source's display title, falling back to its host when no title is
 * present but a URI is.
 */
fun BSkyExternal.sourceDisplayTitle(): String? {
    source?.title?.takeIf { it.isNotBlank() }?.let { return it }
    val uri = source?.uri ?: return null
    return try {
        java.net.URI(uri).host ?: uri
    } catch (_: Exception) {
        uri
    }
}
