/**
 * Inkwell design tokens and Material 3 theme.
 *
 * Brand identity: "The Writer's Desk" — a clean, well-lit workspace.
 * Single green accent (#139500) used sparingly on a pure white/black
 * ink/paper palette. Typography-first; colour recedes so the words can speak.
 *
 * Color tokens align with iOS system colours: pure white page backgrounds
 * with secondarySystemGroupedBackground for cards, pure black/white label
 * text, and the single brand green #139500 accent. Every colour is a
 * light/dark pair, defined once per token.
 */
package uk.ewancroft.inkwell.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.atproto.RgbColor

// ── Brand Tokens ─────────────────────────────────────────────────────────
//
// Canonical brand green: #139500 (Display P3: 0.07611, 0.58470, 0.00000).
// The ink/paper palette uses pure white and pure black so the single green
// accent reads vividly against both light and dark surfaces.

/** Canonical Inkwell brand green. */
val InkwellGreen = Color(0xFF139500)

/** Slightly lighter green for dark-mode accent visibility. */
val InkwellGreenLight = Color(0xFF2DB84D)

/** Tinted green for subtle accent backgrounds. */
val InkwellGreenTint = Color(0xFFE8F5E0)

// ── Ink (text) palette ──
// Aligns with iOS system colours: label is #000000 on light, #FFFFFF on dark.
// Secondary/tertiary text uses fixed muted tones approximating iOS
// secondaryLabel (~48% opacity) and tertiaryLabel (~36% opacity).
private val InkLight = Color(0xFF000000)       // ink-900: strong emphasis (iOS label)
private val InkBodyLight = Color(0xFF000000)   // body text (iOS label)
private val InkMutedLight = Color(0xFF6E6E7E)  // ink-600: secondary (iOS secondaryLabel)
private val InkBorderLight = Color(0xFFC5C5CC) // ink-300: borders (iOS separator)

private val InkDark = Color(0xFFFFFFFF)        // ink-900 dark: emphasis (iOS label)
private val InkBodyDark = Color(0xFFFFFFFF)    // body text (iOS label)
private val InkMutedDark = Color(0xFF8E8E9A)   // ink-600 dark: secondary (iOS secondaryLabel)
private val InkBorderDark = Color(0xFF38383A)  // ink-300 dark: borders (iOS separator)

// ── Paper (background) palette ──
// Aligns with iOS system colours: systemBackground is pure white/black,
// secondarySystemGroupedBackground for cards is #F2F2F7 / #1C1C1E.
private val PaperLight = Color(0xFFFFFFFF)     // paper-100: page bg (iOS systemBackground)
private val PaperSurfaceLight = Color(0xFFF2F2F7) // paper-200: surface/card (iOS secondarySystemGroupedBackground)
private val PaperRaisedLight = Color(0xFFFFFFFF)  // paper-50: raised (iOS systemBackground)

private val PaperDark = Color(0xFF000000)      // paper-100 dark: page bg (iOS systemBackground)
private val PaperSurfaceDark = Color(0xFF1C1C1E)  // paper-200 dark: surface (iOS secondarySystemGroupedBackground)
private val PaperRaisedDark = Color(0xFF121212)   // paper-50 dark: raised (iOS systemGroupedBackground)

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
 * The user's Settings → Customisation appearance override (light/dark),
 * provided at the app root in MainActivity from
 * CustomisationPreferences.getAppearanceOverride. Null means "follow the
 * system" -- every isSystemInDarkTheme() call site in the app should read
 * `LocalForceDarkTheme.current ?: isSystemInDarkTheme()` instead, so the
 * override actually reaches app chrome, reader theming, and anywhere else
 * that branches on light/dark. Mirrors iOS's `.preferredColorScheme`
 * override at the WindowGroup root.
 */
val LocalForceDarkTheme = compositionLocalOf<Boolean?> { null }

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
    val isDark = LocalForceDarkTheme.current ?: isSystemInDarkTheme()

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
 * Uses the ink/paper brand palette: pure white paper tones for
 * backgrounds, pure black/white ink tones for text, and a single vivid
 * green accent used sparingly. Full light/dark parity.
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
    val isDark = LocalForceDarkTheme.current ?: isSystemInDarkTheme()

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
