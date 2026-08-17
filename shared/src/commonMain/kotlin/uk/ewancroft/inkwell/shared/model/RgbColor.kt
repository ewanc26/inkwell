package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for an opaque RGB colour.
 *
 * Used by theme resolution across both platforms. The native platform
 * types map to/from this when crossing the KMP boundary.
 */
data class RgbColor(
    val type: String = "site.standard.theme.color#rgb",
    val r: Int,
    val g: Int,
    val b: Int
)
