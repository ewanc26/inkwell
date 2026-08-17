package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a publication-level basic theme.
 *
 * Four-colour palette: background, foreground, accent, accentForeground.
 * Maps to Android `BasicTheme` and iOS `BasicDefinition`.
 */
data class BasicTheme(
    val type: String = "site.standard.theme.basic",
    val background: RgbColor,
    val foreground: RgbColor,
    val accent: RgbColor,
    val accentForeground: RgbColor
)
