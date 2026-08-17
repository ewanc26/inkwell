package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a legacy light/dark palette.
 *
 * Older standard.site applications emit this shape instead of the full
 * Leaflet rich theme. Contains hex colour strings for five UI elements.
 */
data class LegacyPalette(
    val background: String? = null,
    val text: String? = null,
    val accent: String? = null,
    val link: String? = null,
    val surfaceHover: String? = null
)
