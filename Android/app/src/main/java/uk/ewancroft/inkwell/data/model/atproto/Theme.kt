/**
 * Theme and preference record shapes for the standard.site + Leaflet lexicons.
 *
 * Two theme tiers: a rich Leaflet theme (colors, fonts, page layout) and a
 * simplified basicTheme (four-color palette only). The rendering layer cascades
 * Leaflet -> basicTheme -> system defaults. Mirror of Inkwell iOS ThemeModels.
 */
package uk.ewancroft.inkwell.data.model.atproto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Leaflet Rich Theme ───────────────────────────────────────────────────

/**
 * Full publication/document theme from the Leaflet lexicon.
 * Supports per-section background, heading/body font selection, page width.
 */
@Serializable
data class PublicationTheme(
    @SerialName("\$type") val type: String = "pub.leaflet.publication#theme",
    val backgroundColor: ColorValue? = null,
    val pageBackground: ColorValue? = null,
    val primary: ColorValue? = null,
    val accentBackground: ColorValue? = null,
    val accentText: ColorValue? = null,
    val pageWidth: Int? = null,
    val showPageBackground: Boolean? = null,
    val headingFont: String? = null,
    val bodyFont: String? = null
)

// ── Basic Theme (Four-Colour) ────────────────────────────────────────────

/**
 * Simplified palette with explicit foreground, background, accent pairs.
 * Used as a fallback when no Leaflet rich theme is available.
 */
@Serializable
data class BasicTheme(
    @SerialName("\$type") val type: String = "site.standard.theme.basic",
    val background: RgbColor,
    val foreground: RgbColor,
    val accent: RgbColor,
    val accentForeground: RgbColor
)

// ── Colour Types ─────────────────────────────────────────────────────────

/** RGBA colour (alpha as percentage 0-100, defaults to 100 = fully opaque). */
@Serializable
data class ColorValue(
    @SerialName("\$type") val type: String = "pub.leaflet.theme.color#rgb",
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int? = null
)

/** RGB colour (no alpha — fully opaque by convention). */
@Serializable
data class RgbColor(
    @SerialName("\$type") val type: String = "site.standard.theme.color#rgb",
    val r: Int,
    val g: Int,
    val b: Int
)

// ── Preferences ─────────────────────────────────────────────────────────

/** Publication-level display toggles. */
@Serializable
data class PublicationPreferences(
    val showInDiscover: Boolean? = null
)

/** Per-document display toggles — each section can be hidden independently. */
@Serializable
data class DocumentPreferences(
    val showComments: Boolean? = null,
    val showMentions: Boolean? = null,
    val showRecommends: Boolean? = null,
    val showPrevNext: Boolean? = null,
    val showInDiscover: Boolean? = null
)
