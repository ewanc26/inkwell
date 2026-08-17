package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a Leaflet rich theme colour value.
 *
 * Supports both RGB and RGBA via the optional alpha channel.
 * Alpha is a percentage 0-100, defaulting to 100 (opaque).
 */
data class ColorValue(
    val type: String = "pub.leaflet.theme.color#rgb",
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int? = null
)
