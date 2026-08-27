package uk.ewancroft.inkwell.util

import android.content.Context

/** Non-secret, on-device reader filtering choices. */
object ModerationPreferences {
    private const val PREFERENCES = "moderation_preferences"
    private const val HIDDEN_LABELS = "hidden_labels"
    private const val HIDDEN_KEYWORDS = "hidden_keywords"

    fun hiddenLabels(context: Context): Set<String> =
        preferences(context).getStringSet(HIDDEN_LABELS, emptySet()).orEmpty()

    fun hiddenKeywords(context: Context): Set<String> =
        preferences(context).getStringSet(HIDDEN_KEYWORDS, emptySet()).orEmpty()

    fun setLabelHidden(context: Context, label: String, hidden: Boolean) {
        val labels = hiddenLabels(context).map(String::lowercase).toMutableSet()
        if (hidden) labels += label.lowercase() else labels -= label.lowercase()
        preferences(context).edit().putStringSet(HIDDEN_LABELS, labels).apply()
    }

    fun addKeyword(context: Context, keyword: String) {
        val normalized = keyword.trim().lowercase()
        if (normalized.isBlank()) return
        preferences(context).edit()
            .putStringSet(HIDDEN_KEYWORDS, hiddenKeywords(context) + normalized)
            .apply()
    }

    fun removeKeyword(context: Context, keyword: String) {
        preferences(context).edit()
            .putStringSet(HIDDEN_KEYWORDS, hiddenKeywords(context) - keyword)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
