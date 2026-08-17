package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a rich Leaflet publication theme.
 *
 * Mirrors the structure of Android `PublicationTheme` and iOS
 * `SiteStandardLexicon.Theme.PublicationTheme`. Supports the older
 * light/dark palette shape via [light] and [dark].
 */
data class PublicationTheme(
    val type: String = "pub.leaflet.publication#theme",
    val backgroundColor: ColorValue? = null,
    val pageBackground: ColorValue? = null,
    val primary: ColorValue? = null,
    val accentBackground: ColorValue? = null,
    val accentText: ColorValue? = null,
    val pageWidth: Int? = null,
    val showPageBackground: Boolean? = null,
    val headingFont: String? = null,
    val bodyFont: String? = null,
    val font: String? = null,
    val light: LegacyPalette? = null,
    val dark: LegacyPalette? = null
)
