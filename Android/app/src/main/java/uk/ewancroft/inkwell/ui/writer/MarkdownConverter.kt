package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.*
import uk.ewancroft.inkwell.shared.content.ContentFormatDispatcher
import uk.ewancroft.inkwell.shared.content.JsonMapBridge

/**
 * Converts markdown to format-specific AT Protocol content JSON.
 *
 * Delegates all block-type mapping to the shared KMP converters
 * in `ContentFormatDispatcher`, then bridges the generic Map result
 * to kotlinx.serialization `JsonObject` for the Android platform layer.
 *
 * Previously contained ~300 lines of per-format block mapping duplicated
 * from iOS `ContentProvider.swift`. Now a thin adapter.
 */
object MarkdownConverter {

    fun convert(markdown: String, format: String, uploadedBlobs: Map<String, JsonObject> = emptyMap()): JsonObject {
        val blobs = uploadedBlobs.mapValues { (_, json) -> JsonMapBridge.jsonToMap(json) }
        val result = ContentFormatDispatcher.fromMarkdown(markdown, format, blobs)
        return JsonMapBridge.mapToJson(result.content)
    }
}
