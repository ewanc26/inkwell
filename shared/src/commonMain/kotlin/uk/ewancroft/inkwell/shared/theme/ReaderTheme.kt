package uk.ewancroft.inkwell.shared.theme

import kotlin.math.pow

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
            // User-set overrides from paid customisation, highest priority --
            // deliberately override even a publication's own rich theme,
            // since the point is "read everything the way *you* want",
            // not just a fallback for publications that set nothing.
            overrideAccentRgb: Int? = null,
            overrideFontFamily: FontFamily? = null,
            // Accessibility overrides -- free, unlike the two above, and
            // applied last since a publication's aesthetic choices should
            // never be able to produce illegible text.
            increaseContrast: Boolean = false,
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

            var foreground = firstNonNullInt(
                richPrimaryColor,
                paletteText?.let { hexToRgb(it) },
                basicForeground?.let { hexToRgb(it) },
                0xFF1A1A1A.toInt()
            )

            val accent = overrideAccentRgb ?: firstNonNullInt(
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

            if (increaseContrast || contrastRatio(foreground, background) < 4.5) {
                // Snap to pure black/white rather than compute a target
                // contrast ratio. Accessibility explicitly requests this;
                // otherwise it is a safety net for a publication palette that
                // would make ordinary reader text illegible on its background.
                foreground = if (isPerceptuallyDark(background)) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            }

            val pageWidth = (richPageWidth ?: 680).coerceIn(320, 1000)

            return SharedReaderTheme(
                backgroundRgb = background,
                pageBackgroundRgb = pageBackground,
                foregroundRgb = foreground,
                accentRgb = accent,
                accentForegroundRgb = accentForeground,
                pageWidthDp = pageWidth,
                showPageBackground = richShowPageBackground ?: false,
                headingFontFamily = overrideFontFamily ?: fontFamilyFor(richHeadingFont ?: richSharedFont),
                bodyFontFamily = overrideFontFamily ?: fontFamilyFor(richBodyFont ?: richSharedFont),
            )
        }

        /** Standard perceived-luminance formula (Rec. 601): true below the
         *  midpoint, i.e. a color dark enough to need light content on top
         *  of it. [rgbInt] is 0xRRGGBB, matching this file's Int colour
         *  convention. */
        fun isPerceptuallyDark(rgbInt: Int): Boolean {
            val r = (rgbInt shr 16) and 0xFF
            val g = (rgbInt shr 8) and 0xFF
            val b = rgbInt and 0xFF
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
            return luminance < 0.5
        }

        /** WCAG relative-luminance contrast ratio for two 0xRRGGBB colours. */
        fun contrastRatio(first: Int, second: Int): Double {
            val firstLuminance = relativeLuminance(first)
            val secondLuminance = relativeLuminance(second)
            return (maxOf(firstLuminance, secondLuminance) + 0.05) /
                (minOf(firstLuminance, secondLuminance) + 0.05)
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

        private fun relativeLuminance(rgbInt: Int): Double {
            fun channel(value: Int): Double {
                val normalized = value / 255.0
                return if (normalized <= 0.04045) {
                    normalized / 12.92
                } else {
                    ((normalized + 0.055) / 1.055).pow(2.4)
                }
            }

            val red = channel((rgbInt shr 16) and 0xFF)
            val green = channel((rgbInt shr 8) and 0xFF)
            val blue = channel(rgbInt and 0xFF)
            return 0.2126 * red + 0.7152 * green + 0.0722 * blue
        }
    }
}
