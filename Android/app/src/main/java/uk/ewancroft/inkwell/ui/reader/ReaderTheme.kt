package uk.ewancroft.inkwell.ui.reader

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.atproto.RgbColor
import kotlin.math.min
import kotlin.math.max

/**
 * Resolved publication theme — cascading from Leaflet's rich theme through
 * the basicTheme fallback to system defaults. Mirrors iOS ReaderTheme.swift.
 */
@Immutable
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

        /**
         * Resolves a publication/document's visual identity from the richest
         * available source down to system defaults.
         *
         * Resolution chain: Leaflet rich theme -> legacy palette -> basicTheme -> system.
         */
        @Stable
        fun resolve(
            documentTheme: PublicationTheme? = null,
            publicationTheme: PublicationTheme? = null,
            basicTheme: BasicTheme? = null,
            isDarkTheme: Boolean,
        ): ReaderTheme {
            val rich = documentTheme ?: publicationTheme
            val palette = if (isDarkTheme) rich?.dark else rich?.light

            return ReaderTheme(
                background = rich?.backgroundColor?.toColor()
                    ?: palette?.background?.hexToColor()
                    ?: basicTheme?.background?.toColor()
                    ?: Color.Unspecified,
                pageBackground = rich?.pageBackground?.toColor()
                    ?: palette?.surfaceHover?.hexToColor()
                    ?: rich?.backgroundColor?.toColor()
                    ?: palette?.background?.hexToColor()
                    ?: basicTheme?.background?.toColor()
                    ?: Color.Unspecified,
                foreground = rich?.primary?.toColor()
                    ?: palette?.text?.hexToColor()
                    ?: basicTheme?.foreground?.toColor()
                    ?: Color.Unspecified,
                accent = rich?.accentBackground?.toColor()
                    ?: palette?.link?.hexToColor()
                    ?: palette?.accent?.hexToColor()
                    ?: basicTheme?.accent?.toColor()
                    ?: Color.Unspecified,
                accentForeground = rich?.accentText?.toColor()
                    ?: basicTheme?.accentForeground?.toColor()
                    ?: Color.Unspecified,
                pageWidthDp = min(max(rich?.pageWidth ?: 680, 320), 1000),
                showPageBackground = rich?.showPageBackground ?: false,
                headingFontFamily = resolveFontFamily(rich?.headingFont ?: rich?.font),
                bodyFontFamily = resolveFontFamily(rich?.bodyFont ?: rich?.font),
            )
        }

        /**
         * Maps a Leaflet font identifier to a [FontFamily]. Returns [FontFamily.Sans]
         * when no font was specified — theming should be opt-in, driven entirely by
         * what the publication actually set.
         */
        private fun resolveFontFamily(identifier: String?): FontFamily {
            val value = identifier?.lowercase() ?: return FontFamily.Sans
            return when {
                value.contains("mono") || value.contains("quattro") || value.contains("code") -> FontFamily.Monospaced
                value.contains("lora") || value.contains("newsreader") || value.contains("serif") || value.contains("georgia") -> FontFamily.Serif
                value.contains("atkinson") || value.contains("rounded") -> FontFamily.Rounded
                else -> FontFamily.Sans
            }
        }
    }
}

// ── Colour Conversion Helpers ─────────────────────────────────────────────

/** Converts a Leaflet [ColorValue] (0-255 RGB, 0-100 alpha) to Compose [Color]. */
fun ColorValue.toColor(): Color {
    val alpha = (a ?: 100) / 100f
    return Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = alpha)
}

/** Converts a basic theme [RgbColor] (0-255 RGB) to Compose [Color]. */
fun RgbColor.toColor(): Color {
    return Color(red = r / 255f, green = g / 255f, blue = b / 255f)
}

/** Parses a hex colour string (#RRGGBB or RRGGBB) to Compose [Color]. Returns null for invalid input. */
fun String?.hexToColor(): Color? {
    if (this.isNullOrBlank()) return null
    val value = this.trim().removePrefix("#")
    if (value.length != 6) return null
    return try {
        val rgb = value.toLong(16)
        Color(
            red = ((rgb shr 16) and 0xFF) / 255f,
            green = ((rgb shr 8) and 0xFF) / 255f,
            blue = (rgb and 0xFF) / 255f,
        )
    } catch (_: Exception) {
        null
    }
}
