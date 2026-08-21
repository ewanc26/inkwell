package uk.ewancroft.inkwell.shared.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ReaderThemeTest {

    // ── fontFamilyFor ───────────────────────────────────────────────────

    @Test
    fun fontFamilyForNilReturnsSans() {
        assertEquals(SharedReaderTheme.FontFamily.Sans, SharedReaderTheme.fontFamilyFor(null))
    }

    @Test
    fun fontFamilyForEmptyReturnsSans() {
        assertEquals(SharedReaderTheme.FontFamily.Sans, SharedReaderTheme.fontFamilyFor(""))
    }

    @Test
    fun fontFamilyForSerifKeywords() {
        assertEquals(SharedReaderTheme.FontFamily.Serif, SharedReaderTheme.fontFamilyFor("Lora"))
        assertEquals(SharedReaderTheme.FontFamily.Serif, SharedReaderTheme.fontFamilyFor("NewsReader"))
        assertEquals(SharedReaderTheme.FontFamily.Serif, SharedReaderTheme.fontFamilyFor("serif"))
        assertEquals(SharedReaderTheme.FontFamily.Serif, SharedReaderTheme.fontFamilyFor("Georgia"))
    }

    @Test
    fun fontFamilyForMonospacedKeywords() {
        assertEquals(SharedReaderTheme.FontFamily.Monospaced, SharedReaderTheme.fontFamilyFor("mono"))
        assertEquals(SharedReaderTheme.FontFamily.Monospaced, SharedReaderTheme.fontFamilyFor("Quattro"))
        assertEquals(SharedReaderTheme.FontFamily.Monospaced, SharedReaderTheme.fontFamilyFor("code"))
    }

    @Test
    fun fontFamilyForRoundedKeywords() {
        assertEquals(SharedReaderTheme.FontFamily.Rounded, SharedReaderTheme.fontFamilyFor("Atkinson"))
        assertEquals(SharedReaderTheme.FontFamily.Rounded, SharedReaderTheme.fontFamilyFor("rounded"))
    }

    @Test
    fun fontFamilyForUnknownReturnsSans() {
        assertEquals(SharedReaderTheme.FontFamily.Sans, SharedReaderTheme.fontFamilyFor("ComicSans"))
    }

    // ── hexToRgb ───────────────────────────────────────────────────────

    @Test
    fun hexToRgbWithHash() {
        assertEquals(0xFFFFFF, SharedReaderTheme.hexToRgb("#ffffff"))
        assertEquals(0x000000, SharedReaderTheme.hexToRgb("#000000"))
        assertEquals(0xFF0000, SharedReaderTheme.hexToRgb("#ff0000"))
    }

    @Test
    fun hexToRgbWithoutHash() {
        assertEquals(0xFFFFFF, SharedReaderTheme.hexToRgb("ffffff"))
    }

    @Test
    fun hexToRgbRejectsShortAndLong() {
        assertNull(SharedReaderTheme.hexToRgb("fff"))
        assertNull(SharedReaderTheme.hexToRgb("fffffff"))
        assertNull(SharedReaderTheme.hexToRgb(""))
    }

    @Test
    fun hexToRgbRejectsNonHex() {
        assertNull(SharedReaderTheme.hexToRgb("gggggg"))
    }

    // ── resolve cascade ────────────────────────────────────────────────

    @Test
    fun resolveUsesRichThemeWhenAvailable() {
        val theme = SharedReaderTheme.resolve(
            richBackgroundColor = 0xFFFFFF,
            richPrimaryColor = 0x000000,
            richAccentBackgroundColor = 0x007AFF,
        )
        assertEquals(0xFFFFFF, theme.backgroundRgb)
        assertEquals(0x000000, theme.foregroundRgb)
        assertEquals(0x007AFF, theme.accentRgb)
    }

    @Test
    fun resolveFallsBackToBasicTheme() {
        val theme = SharedReaderTheme.resolve(
            basicBackground = "#ffffff",
            basicForeground = "#333333",
            basicAccent = "#007AFF",
        )
        assertEquals(0xFFFFFF, theme.backgroundRgb)
        assertEquals(0x333333, theme.foregroundRgb)
    }

    @Test
    fun resolveFallsBackToSystemDefaults() {
        val theme = SharedReaderTheme.resolve()
        assertEquals(0xFFF5F5F5.toInt(), theme.backgroundRgb)
        assertEquals(0xFF1A1A1A.toInt(), theme.foregroundRgb)
        assertEquals(0xFF007AFF.toInt(), theme.accentRgb)
    }

    @Test
    fun resolvePageWidthClamped() {
        val tooSmall = SharedReaderTheme.resolve(richPageWidth = 100)
        assertEquals(320, tooSmall.pageWidthDp)

        val tooLarge = SharedReaderTheme.resolve(richPageWidth = 2000)
        assertEquals(1000, tooLarge.pageWidthDp)

        val justRight = SharedReaderTheme.resolve(richPageWidth = 680)
        assertEquals(680, justRight.pageWidthDp)
    }

    @Test
    fun resolveShowPageBackgroundDefaultsFalse() {
        val theme = SharedReaderTheme.resolve()
        assertFalse(theme.showPageBackground)
        val theme2 = SharedReaderTheme.resolve(richShowPageBackground = true)
        assertTrue(theme2.showPageBackground)
    }

    // ── customisation overrides ────────────────────────────────────────

    @Test
    fun overrideAccentWinsOverRichTheme() {
        val theme = SharedReaderTheme.resolve(
            richAccentBackgroundColor = 0x007AFF,
            overrideAccentRgb = 0xFF3B30,
        )
        assertEquals(0xFF3B30, theme.accentRgb)
    }

    @Test
    fun overrideAccentWinsOverEveryTierIncludingSystemDefault() {
        val theme = SharedReaderTheme.resolve(overrideAccentRgb = 0xAF52DE)
        assertEquals(0xAF52DE, theme.accentRgb)
    }

    @Test
    fun noOverrideAccentFallsBackToRichTheme() {
        val theme = SharedReaderTheme.resolve(richAccentBackgroundColor = 0x007AFF)
        assertEquals(0x007AFF, theme.accentRgb)
    }

    @Test
    fun overrideFontFamilyAppliesToBothHeadingAndBody() {
        val theme = SharedReaderTheme.resolve(
            richHeadingFont = "Lora",
            richBodyFont = "Quattro",
            overrideFontFamily = SharedReaderTheme.FontFamily.Rounded,
        )
        assertEquals(SharedReaderTheme.FontFamily.Rounded, theme.headingFontFamily)
        assertEquals(SharedReaderTheme.FontFamily.Rounded, theme.bodyFontFamily)
    }

    @Test
    fun noOverrideFontFamilyFallsBackToRichFonts() {
        val theme = SharedReaderTheme.resolve(richHeadingFont = "Lora", richBodyFont = "Quattro")
        assertEquals(SharedReaderTheme.FontFamily.Serif, theme.headingFontFamily)
        assertEquals(SharedReaderTheme.FontFamily.Monospaced, theme.bodyFontFamily)
    }

    // ── accessibility: isPerceptuallyDark ─────────────────────────────

    @Test
    fun isPerceptuallyDarkForBlackAndWhite() {
        assertTrue(SharedReaderTheme.isPerceptuallyDark(0x000000))
        assertFalse(SharedReaderTheme.isPerceptuallyDark(0xFFFFFF))
    }

    @Test
    fun isPerceptuallyDarkForMidTones() {
        assertFalse(SharedReaderTheme.isPerceptuallyDark(0xF5F5F5)) // near-white default background
        assertTrue(SharedReaderTheme.isPerceptuallyDark(0x1A1A1A)) // near-black default foreground
    }

    // ── accessibility: increaseContrast ────────────────────────────────

    @Test
    fun increaseContrastSnapsForegroundToWhiteOnDarkBackground() {
        val theme = SharedReaderTheme.resolve(
            richBackgroundColor = 0x000000,
            richPrimaryColor = 0x888888, // low-contrast grey a publication might pick
            increaseContrast = true,
        )
        // 0xFFFFFFFF.toInt(), matching this function's existing convention
        // for opaque fallback colours (see accentForeground's default).
        assertEquals(0xFFFFFFFF.toInt(), theme.foregroundRgb)
    }

    @Test
    fun increaseContrastSnapsForegroundToBlackOnLightBackground() {
        val theme = SharedReaderTheme.resolve(
            richBackgroundColor = 0xFFFFFF,
            richPrimaryColor = 0xAAAAAA,
            increaseContrast = true,
        )
        assertEquals(0xFF000000.toInt(), theme.foregroundRgb)
    }

    @Test
    fun noIncreaseContrastKeepsRichForeground() {
        val theme = SharedReaderTheme.resolve(
            richBackgroundColor = 0x000000,
            richPrimaryColor = 0x888888,
        )
        assertEquals(0x888888, theme.foregroundRgb)
    }
}
