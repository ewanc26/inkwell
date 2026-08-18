package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.markdown.MarkdownBlock

/**
 * Result of converting stored content to markdown.
 *
 * Mirrors iOS `ConvertResult` and Android `ConvertResult` in PcktOffprintConverter.kt.
 */
data class SharedConvertResult(
    val blocks: List<MarkdownBlock>,
    val lost: Set<String> = emptySet(),
)

/**
 * Result of converting markdown to format-specific content.
 *
 * Uses generic maps to represent JSON-like structures that both platforms
 * can consume: Android uses kotlinx.serialization `JsonObject`, iOS can
 * bridge from `[String: Any]` dictionaries.
 */
data class SharedWriteResult(
    val content: Map<String, Any?>,
    val lost: Set<String> = emptySet(),
)
