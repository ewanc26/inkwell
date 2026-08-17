package uk.ewancroft.inkwell.shared.theme

/**
 * Shared reader theme resolution — the cascade logic and font-family matching
 * that is identical on both platforms.
 *
 * Colors are stored as 0xRRGGBB Ints (no alpha) so both platforms can convert
 * them to their native Color type. The platform-specific ReaderTheme wrappers
 * call [resolve] and map the Ints to Compose Color / SwiftUI Color.
 *
 * Mirrors iOS `ReaderTheme` and Android `ReaderTheme` — same cascade:
 * Leaflet rich theme → legacy palette → basicTheme → system defaults.
 */
data class SharedReaderTheme(
    val backgroundRgb: Int,
    val pageBackgroundRgb: Int,
    val foregroundRgb: Int,
    val accentRgb: Int,
    val accentForegroundRgb: Int,
    val pageWidthDp: Int,
    val showPageBackground: Boolean,
    val headingFontFamily: FontFamily,
    val bodyFontFamily: FontFamily,
) {

    enum class FontFamily { Sans, Serif, Rounded, Monospaced }

    companion object {

        fun resolve(
            richBackgroundColor: Int? = null,
            richPageBackgroundColor: Int? = null,
            richPrimaryColor: Int? = null,
            richAccentBackgroundColor: Int? = null,
            richAccentTextColor: Int? = null,
            richPageWidth: Int? = null,
            richShowPageBackground: Boolean? = null,
            richHeadingFont: String? = null,
            richBodyFont: String? = null,
            richSharedFont: String? = null,
            paletteBackground: String? = null,
            paletteText: String? = null,
            paletteLink: String? = null,
            paletteAccent: String? = null,
            paletteSurfaceHover: String? = null,
            basicBackground: String? = null,
            basicForeground: String? = null,
            basicAccent: String? = null,
            basicAccentForeground: String? = null,
        ): SharedReaderTheme {
            val background = firstNonNullInt(
                richBackgroundColor,
                paletteBackground?.let { hexToRgb(it) },
                basicBackground?.let { hexToRgb(it) },
                0xFFF5F5F5.toInt()
            )

            val pageBackground = firstNonNullInt(
                richPageBackgroundColor,
                paletteSurfaceHover?.let { hexToRgb(it) },
                background
            )

            val foreground = firstNonNullInt(
                richPrimaryColor,
                paletteText?.let { hexToRgb(it) },
                basicForeground?.let { hexToRgb(it) },
                0xFF1A1A1A.toInt()
            )

            val accent = firstNonNullInt(
                richAccentBackgroundColor,
                paletteLink?.let { hexToRgb(it) },
                paletteAccent?.let { hexToRgb(it) },
                basicAccent?.let { hexToRgb(it) },
                0xFF007AFF.toInt()
            )

            val accentForeground = firstNonNullInt(
                richAccentTextColor,
                basicAccentForeground?.let { hexToRgb(it) },
                0xFFFFFFFF.toInt()
            )

            val pageWidth = (richPageWidth ?: 680).coerceIn(320, 1000)

            return SharedReaderTheme(
                backgroundRgb = background,
                pageBackgroundRgb = pageBackground,
                foregroundRgb = foreground,
                accentRgb = accent,
                accentForegroundRgb = accentForeground,
                pageWidthDp = pageWidth,
                showPageBackground = richShowPageBackground ?: false,
                headingFontFamily = fontFamilyFor(richHeadingFont ?: richSharedFont),
                bodyFontFamily = fontFamilyFor(richBodyFont ?: richSharedFont),
            )
        }

        fun fontFamilyFor(identifier: String?): FontFamily {
            val value = identifier?.lowercase() ?: return FontFamily.Sans
            return when {
                value.contains("mono") || value.contains("quattro") || value.contains("code") -> FontFamily.Monospaced
                value.contains("lora") || value.contains("newsreader") || value.contains("serif") || value.contains("georgia") -> FontFamily.Serif
                value.contains("atkinson") || value.contains("rounded") -> FontFamily.Rounded
                else -> FontFamily.Sans
            }
        }

        fun hexToRgb(hex: String): Int? {
            val cleaned = hex.removePrefix("#")
            if (cleaned.length != 6) return null
            return cleaned.toIntOrNull(16)
        }

        private fun firstNonNullInt(vararg candidates: Int?): Int {
            for (c in candidates) {
                if (c != null) return c
            }
            error("No non-null candidate provided")
        }
    }
}
