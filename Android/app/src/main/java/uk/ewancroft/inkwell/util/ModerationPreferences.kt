package uk.ewancroft.inkwell.util

import android.content.Context

/** Non-secret, on-device reader filtering choices. */
object ModerationPreferences {
    private const val PREFERENCES = "moderation_preferences"
    private const val HIDDEN_LABELS = "hidden_labels"
    private const val WARNING_LABELS = "warning_labels"
    private const val CUSTOM_LABELS = "custom_labels"
    private const val KNOWN_LABELERS = "known_labelers"
    private const val DISABLED_LABELERS = "disabled_labelers"
    private const val HIDDEN_KEYWORDS = "hidden_keywords"

    fun hiddenLabels(context: Context): Set<String> =
        preferences(context).getStringSet(HIDDEN_LABELS, emptySet()).orEmpty()

    fun hiddenKeywords(context: Context): Set<String> =
        preferences(context).getStringSet(HIDDEN_KEYWORDS, emptySet()).orEmpty()

    fun warningLabels(context: Context): Set<String> =
        preferences(context).getStringSet(WARNING_LABELS, emptySet()).orEmpty()

    fun customLabels(context: Context): Set<String> =
        preferences(context).getStringSet(CUSTOM_LABELS, emptySet()).orEmpty()

    fun knownLabelers(context: Context): Set<String> =
        preferences(context).getStringSet(KNOWN_LABELERS, emptySet()).orEmpty()

    fun disabledLabelers(context: Context): Set<String> =
        preferences(context).getStringSet(DISABLED_LABELERS, emptySet()).orEmpty()

    fun setLabelHidden(context: Context, label: String, hidden: Boolean) {
        val labels = hiddenLabels(context).map(String::lowercase).toMutableSet()
        if (hidden) labels += label.lowercase() else labels -= label.lowercase()
        preferences(context).edit().putStringSet(HIDDEN_LABELS, labels).apply()
    }

    fun labelMode(context: Context, label: String): LabelMode {
        val normalized = label.normalizedOrNull() ?: return LabelMode.Show
        return when {
            normalized in hiddenLabels(context).normalized() -> LabelMode.Hide
            normalized in warningLabels(context).normalized() -> LabelMode.Warn
            else -> LabelMode.Show
        }
    }

    fun setLabelMode(context: Context, label: String, mode: LabelMode) {
        val normalized = label.normalizedOrNull() ?: return
        val hidden = hiddenLabels(context).normalized().toMutableSet()
        val warnings = warningLabels(context).normalized().toMutableSet()
        hidden -= normalized
        warnings -= normalized
        when (mode) {
            LabelMode.Show -> Unit
            LabelMode.Warn -> warnings += normalized
            LabelMode.Hide -> hidden += normalized
        }
        preferences(context).edit()
            .putStringSet(HIDDEN_LABELS, hidden)
            .putStringSet(WARNING_LABELS, warnings)
            .apply()
    }

    fun addCustomLabel(context: Context, label: String) {
        val normalized = label.normalizedOrNull() ?: return
        preferences(context).edit()
            .putStringSet(CUSTOM_LABELS, customLabels(context).normalized() + normalized)
            .apply()
    }

    fun removeCustomLabel(context: Context, label: String) {
        val normalized = label.normalizedOrNull() ?: return
        preferences(context).edit()
            .putStringSet(CUSTOM_LABELS, customLabels(context).normalized() - normalized)
            .apply()
    }

    fun setLabelerEnabled(context: Context, labeler: String, enabled: Boolean) {
        val normalized = labeler.normalizedOrNull() ?: return
        val known = knownLabelers(context).normalized() + normalized
        val disabled = disabledLabelers(context).normalized().toMutableSet()
        if (enabled) disabled -= normalized else disabled += normalized
        preferences(context).edit()
            .putStringSet(KNOWN_LABELERS, known)
            .putStringSet(DISABLED_LABELERS, disabled)
            .apply()
    }

    fun removeLabeler(context: Context, labeler: String) {
        val normalized = labeler.normalizedOrNull() ?: return
        preferences(context).edit()
            .putStringSet(KNOWN_LABELERS, knownLabelers(context).normalized() - normalized)
            .putStringSet(DISABLED_LABELERS, disabledLabelers(context).normalized() - normalized)
            .apply()
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

    private fun Set<String>.normalized(): Set<String> = mapNotNull { it.normalizedOrNull() }.toSet()

    private fun String.normalizedOrNull(): String? =
        trim().lowercase().takeIf(String::isNotEmpty)
}

enum class LabelMode {
    Show,
    Warn,
    Hide,
}
