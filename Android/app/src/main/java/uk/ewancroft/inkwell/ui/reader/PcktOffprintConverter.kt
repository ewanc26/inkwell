package uk.ewancroft.inkwell.ui.reader

import kotlinx.serialization.json.JsonObject
import uk.ewancroft.inkwell.shared.content.ContentFormatDispatcher
import uk.ewancroft.inkwell.shared.content.JsonMapBridge
import uk.ewancroft.inkwell.shared.markdown.MarkdownSerializer

data class ConvertResult(
    val markdown: String?,
    val lost: List<String> = emptyList(),
)

/**
 * Converts pckt (blog.pckt.content) and Offprint (app.offprint.content)
 * block-array records into markdown strings.
 *
 * Delegates all block-type mapping to the shared KMP converters in
 * `ContentFormatDispatcher`, then bridges from kotlinx.serialization
 * `JsonObject` to the generic Map representation.
 *
 * Previously contained ~300 lines of per-format block mapping duplicated
 * from iOS `ContentProvider.swift`. Now a thin adapter.
 */
object PcktOffprintConverter {

    fun isSupported(formatType: String?): Boolean =
        ContentFormatDispatcher.isPcktOrOffprint(formatType)

    fun toMarkdown(contentObj: JsonObject, formatType: String, authorDid: String = ""): ConvertResult {
        val contentMap = JsonMapBridge.jsonToMap(contentObj)
        val result = ContentFormatDispatcher.toMarkdown(contentMap, authorDid)

        if (result.blocks.isEmpty()) return ConvertResult(null, result.lost.toList())

        val markdown = MarkdownSerializer.serialize(result.blocks)
        return ConvertResult(markdown, result.lost.toList())
    }
}
