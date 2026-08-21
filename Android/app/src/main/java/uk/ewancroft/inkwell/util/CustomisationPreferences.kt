package uk.ewancroft.inkwell.util

import android.content.Context
import uk.ewancroft.inkwell.shared.theme.SharedReaderTheme

/**
 * User-chosen appearance overrides, unlocked by a paid license key (see
 * LicenseVerifier.kt). Deliberately takes priority over whatever a
 * publication's own theme sets -- the point is reading everything the
 * way *you* want, not just a fallback for publications that set nothing.
 *
 * Context-parameterised rather than Hilt-injected to match
 * TipPromptManager's existing convention for lightweight preference
 * reads outside the DI graph. Mirrors iOS CustomisationSettings.swift.
 */
object CustomisationPreferences {
    private const val PREFS_NAME = "inkwell_customisation"
    private const val UNLOCKED_KEY = "unlocked"
    private const val ACCENT_COLOR_HEX_KEY = "accent_color_hex"
    private const val FONT_FAMILY_KEY = "font_family"
    private const val APPEARANCE_OVERRIDE_KEY = "appearance_override"

    enum class AppearanceOverride { LIGHT, DARK }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isUnlocked(context: Context): Boolean = prefs(context).getBoolean(UNLOCKED_KEY, false)

    /** Returns true and persists the unlock if the key verifies. */
    fun unlock(context: Context, key: String): Boolean {
        if (!LicenseVerifier.isValid(key)) return false
        prefs(context).edit().putBoolean(UNLOCKED_KEY, true).apply()
        return true
    }

    fun getAccentColorHex(context: Context): String? = prefs(context).getString(ACCENT_COLOR_HEX_KEY, null)

    fun setAccentColorHex(context: Context, hex: String?) {
        prefs(context).edit().putString(ACCENT_COLOR_HEX_KEY, hex).apply()
    }

    /** 0xRRGGBB, matching SharedReaderTheme's Int colour convention. */
    fun getAccentColorRgbInt(context: Context): Int? {
        val hex = getAccentColorHex(context)?.removePrefix("#") ?: return null
        if (hex.length != 6) return null
        return hex.toIntOrNull(16)
    }

    fun getFontFamilyOverride(context: Context): SharedReaderTheme.FontFamily? =
        prefs(context).getString(FONT_FAMILY_KEY, null)?.let {
            try { SharedReaderTheme.FontFamily.valueOf(it) } catch (e: IllegalArgumentException) { null }
        }

    fun setFontFamilyOverride(context: Context, family: SharedReaderTheme.FontFamily?) {
        prefs(context).edit().putString(FONT_FAMILY_KEY, family?.name).apply()
    }

    /** null means "follow the system". */
    fun getAppearanceOverride(context: Context): AppearanceOverride? =
        prefs(context).getString(APPEARANCE_OVERRIDE_KEY, null)?.let {
            try { AppearanceOverride.valueOf(it) } catch (e: IllegalArgumentException) { null }
        }

    fun setAppearanceOverride(context: Context, override: AppearanceOverride?) {
        prefs(context).edit().putString(APPEARANCE_OVERRIDE_KEY, override?.name).apply()
    }
}
