package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for per-document display preferences.
 */
data class DocumentPreferences(
    val showComments: Boolean? = null,
    val showMentions: Boolean? = null,
    val showRecommends: Boolean? = null,
    val showPrevNext: Boolean? = null,
    val showInDiscover: Boolean? = null
)
