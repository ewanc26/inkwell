package uk.ewancroft.inkwell.shared.moderation

import kotlinx.serialization.Serializable

/** A moderation label supplied by a labeler for a document or publication. */
@Serializable
data class ModerationLabel(
    val value: String,
    val source: String? = null,
)

/**
 * Common Bluesky label categories exposed as stable display references and
 * label values. Services can also provide custom values, which the filter
 * supports through [ModerationPolicy] without an app update.
 */
enum class StandardModerationLabel(
    val reference: String,
    val labelValue: String,
) {
    Nsfw("com.atproto.label.defs#nsfw", "nsfw"),
    Gore("com.atproto.label.defs#gore", "gore"),
    Impersonation("com.atproto.label.defs#impersonation", "impersonation"),
    Sexual("com.atproto.label.defs#sexual", "sexual"),
    SelfHarm("com.atproto.label.defs#self-harm", "self-harm"),
}

/** Reader-controlled moderation choices. Values are matched case-insensitively. */
@Serializable
data class ModerationPolicy(
    val hiddenLabels: Set<String> = emptySet(),
    val warningLabels: Set<String> = emptySet(),
    val disabledLabelers: Set<String> = emptySet(),
    val hiddenKeywords: Set<String> = emptySet(),
)

/** Content fields available to the shared moderation decision. */
data class FilterableContent(
    val title: String? = null,
    val description: String? = null,
    val textContent: String? = null,
    val labels: List<ModerationLabel> = emptyList(),
)

enum class FilterMatchKind {
    Label,
    Keyword,
}

data class FilterMatch(
    val kind: FilterMatchKind,
    val value: String,
)

sealed interface ContentFilterDecision {
    data object Show : ContentFilterDecision
    data class Warn(val matches: List<FilterMatch>) : ContentFilterDecision
    data class Hide(val matches: List<FilterMatch>) : ContentFilterDecision
}

/**
 * Pure, deterministic moderation policy evaluator. Network clients provide
 * labels from protocol responses; this class never invents or fetches labels.
 */
object ContentFilterEngine {
    fun evaluate(content: FilterableContent, policy: ModerationPolicy): ContentFilterDecision {
        val hiddenLabels = policy.hiddenLabels.normalized()
        val warningLabels = policy.warningLabels.normalized()
        val disabledLabelers = policy.disabledLabelers.normalized()
        val keywordMatches = policy.hiddenKeywords.normalized().mapNotNull { keyword ->
            keyword.takeIf { content.searchableText().contains(it, ignoreCase = true) }
                ?.let { FilterMatch(FilterMatchKind.Keyword, keyword) }
        }
        val usableLabels = content.labels.filterNot { label ->
            label.source?.normalizedOrNull() in disabledLabelers
        }
        val hiddenMatches = usableLabels.mapNotNull { label ->
            label.value.normalizedOrNull()
                ?.takeIf { it in hiddenLabels }
                ?.let { FilterMatch(FilterMatchKind.Label, it) }
        } + keywordMatches
        if (hiddenMatches.isNotEmpty()) return ContentFilterDecision.Hide(hiddenMatches)

        val warningMatches = usableLabels.mapNotNull { label ->
            label.value.normalizedOrNull()
                ?.takeIf { it in warningLabels }
                ?.let { FilterMatch(FilterMatchKind.Label, it) }
        }
        return if (warningMatches.isEmpty()) ContentFilterDecision.Show
        else ContentFilterDecision.Warn(warningMatches)
    }

    private fun FilterableContent.searchableText(): String =
        listOfNotNull(title, description, textContent).joinToString("\n")

    private fun Iterable<String>.normalized(): Set<String> =
        mapNotNull { it.normalizedOrNull() }.toSet()

    private fun String.normalizedOrNull(): String? = trim().lowercase().takeIf(String::isNotEmpty)
}
