package uk.ewancroft.inkwell.ui.reader

import androidx.compose.ui.graphics.Color
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.atproto.RgbColor
import uk.ewancroft.inkwell.shared.theme.SharedReaderTheme

/**
 * Resolved publication theme — cascading from Leaflet's rich theme through
 * the basicTheme fallback to system defaults. The cascade logic lives in
 * the shared KMP core; this adapter converts Android theme models to/from
 * the shared representation.
 */
data class ReaderTheme(
    val background: Color,
    val pageBackground: Color,
    val foreground: Color,
    val accent: Color,
    val accentForeground: Color,
    val pageWidthDp: Int,
    val showPageBackground: Boolean,
    val headingFontFamily: FontFamily,
    val bodyFontFamily: FontFamily,
) {
    enum class FontFamily {
        Sans, Serif, Rounded, Monospaced
    }

    companion object {

        fun resolve(
            documentTheme: PublicationTheme? = null,
            publicationTheme: PublicationTheme? = null,
            basicTheme: BasicTheme? = null,
            isDarkTheme: Boolean,
            // User-set overrides from paid customisation, highest priority --
            // deliberately override even a publication's own rich theme,
            // since the point is reading everything the way *you* want.
            // Mirrors iOS ReaderTheme.swift threading CustomisationSettings
            // into the same shared resolver call.
            overrideAccentRgb: Int? = null,
            overrideFontFamily: SharedReaderTheme.FontFamily? = null,
            // Accessibility override -- free, unlike the two above.
            increaseContrast: Boolean = false,
        ): ReaderTheme {
            val rich = documentTheme ?: publicationTheme
            val palette = if (isDarkTheme) rich?.dark else rich?.light

            val shared = SharedReaderTheme.resolve(
                richBackgroundColor = rich?.backgroundColor?.toRgbInt(),
                richPageBackgroundColor = rich?.pageBackground?.toRgbInt(),
                richPrimaryColor = rich?.primary?.toRgbInt(),
                richAccentBackgroundColor = rich?.accentBackground?.toRgbInt(),
                richAccentTextColor = rich?.accentText?.toRgbInt(),
                richPageWidth = rich?.pageWidth,
                richShowPageBackground = rich?.showPageBackground,
                richHeadingFont = rich?.headingFont,
                richBodyFont = rich?.bodyFont,
                richSharedFont = rich?.font,
                paletteBackground = palette?.background,
                paletteText = palette?.text,
                paletteLink = palette?.link,
                paletteAccent = palette?.accent,
                paletteSurfaceHover = palette?.surfaceHover,
                basicBackground = basicTheme?.background?.toHexString(),
                basicForeground = basicTheme?.foreground?.toHexString(),
                basicAccent = basicTheme?.accent?.toHexString(),
                basicAccentForeground = basicTheme?.accentForeground?.toHexString(),
                overrideAccentRgb = overrideAccentRgb,
                overrideFontFamily = overrideFontFamily,
                increaseContrast = increaseContrast,
            )

            return ReaderTheme(
                background = shared.backgroundRgb.toColor(),
                pageBackground = shared.pageBackgroundRgb.toColor(),
                foreground = shared.foregroundRgb.toColor(),
                accent = shared.accentRgb.toColor(),
                accentForeground = shared.accentForegroundRgb.toColor(),
                pageWidthDp = shared.pageWidthDp,
                showPageBackground = shared.showPageBackground,
                headingFontFamily = shared.headingFontFamily.toLocal(),
                bodyFontFamily = shared.bodyFontFamily.toLocal(),
            )
        }
    }
}

// ── Colour Conversion Helpers ─────────────────────────────────────────────

private fun ColorValue.toRgbInt(): Int {
    val r = (r * 255).coerceIn(0, 255)
    val g = (g * 255).coerceIn(0, 255)
    val b = (b * 255).coerceIn(0, 255)
    return (r shl 16) or (g shl 8) or b
}

private fun RgbColor.toHexString(): String {
    val r = r.coerceIn(0, 255)
    val g = g.coerceIn(0, 255)
    val b = b.coerceIn(0, 255)
    return String.format("#%02X%02X%02X", r, g, b)
}

/** Reader theme colours are 0xRRGGBB. Compose's Int constructor expects
 * 0xAARRGGBB, so explicitly add an opaque alpha channel before rendering. */
private fun Int.toColor(): Color = Color(0xFF000000.toInt() or (this and 0x00FFFFFF))

private fun SharedReaderTheme.FontFamily.toLocal(): ReaderTheme.FontFamily = when (this) {
    SharedReaderTheme.FontFamily.Sans -> ReaderTheme.FontFamily.Sans
    SharedReaderTheme.FontFamily.Serif -> ReaderTheme.FontFamily.Serif
    SharedReaderTheme.FontFamily.Rounded -> ReaderTheme.FontFamily.Rounded
    SharedReaderTheme.FontFamily.Monospaced -> ReaderTheme.FontFamily.Monospaced
}
