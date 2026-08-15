/**
 * Inkwell design tokens and Material 3 theme.
 *
 * Brand identity: "The Writer's Desk" — a clean, well-lit workspace.
 * Single green accent (#139500) used sparingly on a cool-toned ink/paper
 * palette. Typography-first; colour recedes so the words can speak.
 *
 * Mirrors the OKLCH-based token system in Inkwell iOS (InkwellTheme.swift)
 * and inkwell-website (tokens.css). Every colour is a light/dark pair,
 * defined once per token.
 */
package uk.ewancroft.inkwell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.atproto.RgbColor

// ── Brand Tokens ─────────────────────────────────────────────────────────
//
// Canonical brand green: #139500 (Display P3: 0.07611, 0.58470, 0.00000).
// The ink/paper palette uses cool off-white and cool dark tones so the
// single green accent reads vividly against both light and dark surfaces.

/** Canonical Inkwell brand green. */
val InkwellGreen = Color(0xFF139500)

/** Slightly lighter green for dark-mode accent visibility. */
val InkwellGreenLight = Color(0xFF2DB84D)

/** Tinted green for subtle accent backgrounds. */
val InkwellGreenTint = Color(0xFFE8F5E0)

// ── Ink (text) palette ──
private val InkLight = Color(0xFF1A1A2E)       // ink-900: strong emphasis
private val InkBodyLight = Color(0xFF4A4A5A)   // ink-700: body text
private val InkMutedLight = Color(0xFF6E6E7E)  // ink-600: secondary
private val InkBorderLight = Color(0xFFC5C5CC) // ink-300: borders

private val InkDark = Color(0xFFE8E6F0)        // ink-900 dark: emphasis
private val InkBodyDark = Color(0xFFB5B5C0)    // ink-700 dark: body
private val InkMutedDark = Color(0xFF8E8E9A)   // ink-600 dark: secondary
private val InkBorderDark = Color(0xFF3E3E48)  // ink-300 dark: borders

// ── Paper (background) palette ──
private val PaperLight = Color(0xFFFAFAF5)     // paper-100: page bg
private val PaperSurfaceLight = Color(0xFFF5F5F0) // paper-200: surface/card
private val PaperRaisedLight = Color(0xFFFCFCFA)  // paper-50: raised

private val PaperDark = Color(0xFF1C1C24)      // paper-100 dark: page bg
private val PaperSurfaceDark = Color(0xFF2A2A33)  // paper-200 dark: surface
private val PaperRaisedDark = Color(0xFF141418)   // paper-50 dark: raised

// ── Resolved Theme ───────────────────────────────────────────────────────

/**
 * A fully resolved colour palette ready for Material 3.
 *
 * Document-level theme values override publication-level ones when both exist,
 * mirroring the cascade in standard.site's rendering spec.
 */
data class ResolvedTheme(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val accentForeground: Color,
    val pageBackground: Color,
    val pageWidth: Int = 680,
    val showPageBackground: Boolean = false
)

// ── Theme Resolution ─────────────────────────────────────────────────────

/**
 * Resolves a publication/document theme cascade into a ResolvedTheme.
 *
 * Priority (highest first):
 *   1. Document theme (Leaflet rich)
 *   2. Publication theme (Leaflet rich)
 *   3. Basic theme (4-color palette)
 *   4. Inkwell brand default (ink/paper system)
 */
@Composable
fun resolveTheme(
    publicationTheme: PublicationTheme? = null,
    basicTheme: BasicTheme? = null,
    documentTheme: PublicationTheme? = null
): ResolvedTheme {
    val isDark = isSystemInDarkTheme()

    val accent = documentTheme?.accentBackground?.toColor()
        ?: publicationTheme?.accentBackground?.toColor()
        ?: basicTheme?.accent?.toColor()
        ?: if (isDark) InkwellGreenLight else InkwellGreen

    val foreground = documentTheme?.primary?.toColor()
        ?: publicationTheme?.primary?.toColor()
        ?: basicTheme?.foreground?.toColor()
        ?: if (isDark) InkBodyDark else InkBodyLight

    val background = documentTheme?.backgroundColor?.toColor()
        ?: publicationTheme?.backgroundColor?.toColor()
        ?: basicTheme?.background?.toColor()
        ?: if (isDark) PaperDark else PaperLight

    val pageBg = documentTheme?.pageBackground?.toColor()
        ?: publicationTheme?.pageBackground?.toColor()
        ?: background

    val accentFg = documentTheme?.accentText?.toColor()
        ?: publicationTheme?.accentText?.toColor()
        ?: basicTheme?.accentForeground?.toColor()
        ?: Color.White

    val pageWidth = documentTheme?.pageWidth
        ?: publicationTheme?.pageWidth
        ?: 680

    val showPageBg = documentTheme?.showPageBackground
        ?: publicationTheme?.showPageBackground
        ?: false

    return ResolvedTheme(background, foreground, accent, accentFg, pageBg, pageWidth, showPageBg)
}

// ── Inkwell Theme ────────────────────────────────────────────────────────

/**
 * Material 3 colour scheme for the Inkwell app.
 *
 * Uses the ink/paper brand palette: cool off-white paper tones for
 * backgrounds, cool ink tones for text, and a single vivid green accent
 * used sparingly. Full light/dark parity.
 *
 * When a resolved theme is available (from a publication/document), it maps
 * the document's palette directly to Material 3 colour roles.
 */
@Composable
fun InkwellTheme(
    resolvedTheme: ResolvedTheme? = null,
    content: @Composable () -> Unit
) {
    val theme = resolvedTheme
    val isDark = isSystemInDarkTheme()

    val colorScheme = if (theme != null) {
        // Document/publication theme — map resolved palette to Material 3
        if (isDark) darkColorScheme(
            primary = theme.accent,
            onPrimary = theme.accentForeground,
            background = theme.background,
            onBackground = theme.foreground,
            surface = theme.pageBackground,
            onSurface = theme.foreground,
            surfaceVariant = if (isDark) PaperSurfaceDark else PaperSurfaceLight,
            onSurfaceVariant = if (isDark) InkMutedDark else InkMutedLight,
            outline = if (isDark) InkBorderDark else InkBorderLight,
        ) else lightColorScheme(
            primary = theme.accent,
            onPrimary = theme.accentForeground,
            background = theme.background,
            onBackground = theme.foreground,
            surface = theme.pageBackground,
            onSurface = theme.foreground,
            surfaceVariant = if (isDark) PaperSurfaceDark else PaperSurfaceLight,
            onSurfaceVariant = if (isDark) InkMutedDark else InkMutedLight,
            outline = if (isDark) InkBorderDark else InkBorderLight,
        )
    } else if (isDark) {
        darkColorScheme(
            primary = InkwellGreenLight,
            onPrimary = Color.Black,
            background = PaperDark,
            onBackground = InkBodyDark,
            surface = PaperSurfaceDark,
            onSurface = InkBodyDark,
            surfaceVariant = PaperRaisedDark,
            onSurfaceVariant = InkMutedDark,
            outline = InkBorderDark,
            outlineVariant = InkBorderDark.copy(alpha = 0.5f),
        )
    } else {
        lightColorScheme(
            primary = InkwellGreen,
            onPrimary = Color.White,
            background = PaperLight,
            onBackground = InkBodyLight,
            surface = PaperSurfaceLight,
            onSurface = InkBodyLight,
            surfaceVariant = PaperRaisedLight,
            onSurfaceVariant = InkMutedLight,
            outline = InkBorderLight,
            outlineVariant = InkBorderLight.copy(alpha = 0.5f),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

// ── Colour Conversion Extensions ─────────────────────────────────────────

/**
 * Converts a Leaflet ColorValue (RGBA, alpha 0-100 percentage)
 * to a Compose Color (RGB 0-1 float, alpha 0-1 float).
 */
private fun ColorValue.toColor(): Color =
    Color(
        red = r / 255f,
        green = g / 255f,
        blue = b / 255f,
        alpha = (a ?: 100) / 100f
    )

/**
 * Converts a standard.site RgbColor (fully opaque) to a Compose Color.
 */
private fun RgbColor.toColor(): Color =
    Color(red = r / 255f, green = g / 255f, blue = b / 255f)
