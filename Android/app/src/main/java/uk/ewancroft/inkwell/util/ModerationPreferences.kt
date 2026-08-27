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

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
