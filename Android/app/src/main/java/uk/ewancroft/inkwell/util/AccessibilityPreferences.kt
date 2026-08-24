package uk.ewancroft.inkwell.util

import android.content.Context

/**
 * Accessibility overrides -- always free, unlike the aesthetic overrides
 * in CustomisationPreferences.kt. Gating accessibility behind a paywall
 * is bad practice regardless of Inkwell's own honour-system pricing
 * model, so these live in their own preferences file. Mirrors iOS
 * AccessibilitySettings.swift.
 */
object AccessibilityPreferences {
    private const val PREFS_NAME = "inkwell_accessibility"
    private const val FONT_SIZE_SCALE_KEY = "font_size_scale"
    private const val BOLD_TEXT_KEY = "bold_text"
    private const val INCREASE_CONTRAST_KEY = "increase_contrast"
    private const val UNDERLINE_LINKS_KEY = "underline_links"
    private const val HAPTICS_ENABLED_KEY = "haptics_enabled"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 1.0 is the system default size; the allowed range mirrors iOS's own
     *  larger-text accessibility slider (roughly 0.8x-1.5x). */
    fun getFontSizeScale(context: Context): Float = prefs(context).getFloat(FONT_SIZE_SCALE_KEY, 1.0f)

    fun setFontSizeScale(context: Context, scale: Float) {
        prefs(context).edit().putFloat(FONT_SIZE_SCALE_KEY, scale).apply()
    }

    fun getBoldText(context: Context): Boolean = prefs(context).getBoolean(BOLD_TEXT_KEY, false)

    fun setBoldText(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(BOLD_TEXT_KEY, enabled).apply()
    }

    fun getIncreaseContrast(context: Context): Boolean = prefs(context).getBoolean(INCREASE_CONTRAST_KEY, false)

    fun setIncreaseContrast(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(INCREASE_CONTRAST_KEY, enabled).apply()
    }

    /** Defaults to true, unlike iOS: Android's MarkdownRendererView has
     *  always underlined links, so this preserves existing behaviour
     *  rather than silently changing it -- both platforms default to
     *  whatever they already did before this setting existed. */
    fun getUnderlineLinks(context: Context): Boolean = prefs(context).getBoolean(UNDERLINE_LINKS_KEY, true)

    fun setUnderlineLinks(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(UNDERLINE_LINKS_KEY, enabled).apply()
    }

    /** Mirrors iOS's HapticsSettings, which also defaults to true. */
    fun getHapticsEnabled(context: Context): Boolean = prefs(context).getBoolean(HAPTICS_ENABLED_KEY, true)

    fun setHapticsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(HAPTICS_ENABLED_KEY, enabled).apply()
    }

    fun resetToDefaults(context: Context) {
        prefs(context).edit()
            .putFloat(FONT_SIZE_SCALE_KEY, 1.0f)
            .putBoolean(BOLD_TEXT_KEY, false)
            .putBoolean(INCREASE_CONTRAST_KEY, false)
            .putBoolean(UNDERLINE_LINKS_KEY, true)
            .putBoolean(HAPTICS_ENABLED_KEY, true)
            .apply()
    }
}
