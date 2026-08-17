package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a translucent RGBA colour.
 *
 * Alpha is stored as a percentage 0-100 (100 = fully opaque), matching
 * both the Android and iOS convention.
 */
data class RgbaColor(
    val type: String = "site.standard.theme.color#rgba",
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 100
)
