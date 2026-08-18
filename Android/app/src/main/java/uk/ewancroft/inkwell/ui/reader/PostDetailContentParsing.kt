package uk.ewancroft.inkwell.ui.reader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.content.LeafletContent
import uk.ewancroft.inkwell.data.model.content.LeafletPage
import uk.ewancroft.inkwell.data.repository.downloadBlob
import uk.ewancroft.inkwell.shared.content.ContentFormatDetector

private val contentParsingJson = Json { ignoreUnknownKeys = true; isLenient = true }

internal data class ParseResult(val content: DocumentContent, val lost: List<String> = emptyList())

internal suspend fun PostDetailViewModel.parseContent(
    contentObj: JsonObject?,
    textContent: String?,
    authorDid: String,
    documentUri: String,
): ParseResult {
    if (contentObj != null) {
        val formatType = contentObj["\$type"]?.jsonPrimitive?.contentOrNull

        if (formatType == ContentFormatDetector.LEAFLET) {
            val leaflet = runCatching { contentParsingJson.decodeFromJsonElement<LeafletContent>(contentObj) }.getOrNull()
            var pages = leaflet?.pages
            if (pages.isNullOrEmpty() && leaflet?.blobPages != null) {
                pages = runCatching {
                    val blobData = pdsRepository.downloadBlob(
                        cid = leaflet.blobPages.link,
                        fromDID = authorDid
                    )
                    contentParsingJson.decodeFromString<List<LeafletPage>>(blobData.decodeToString())
                }.getOrNull()
            }
            if (!pages.isNullOrEmpty()) {
                return ParseResult(DocumentContent.Leaflet(pages, authorDid))
            }
        }

        if (formatType == ContentFormatDetector.MARKPUB) {
            val markdown = contentObj["text"]?.jsonObject?.get("markdown")?.jsonPrimitive?.contentOrNull
            if (!markdown.isNullOrBlank()) return ParseResult(DocumentContent.Markdown(markdown))
        }

        if (PcktOffprintConverter.isSupported(formatType)) {
            val result = PcktOffprintConverter.toMarkdown(contentObj, formatType!!, authorDid)
            if (!result.markdown.isNullOrBlank()) return ParseResult(DocumentContent.Markdown(result.markdown), result.lost)
        }

        val extracted = StringBuilder()
        collectPlaintext(contentObj, extracted)
        if (extracted.isNotBlank()) return ParseResult(DocumentContent.PlainText(extracted.toString()))

        if (!textContent.isNullOrBlank()) return ParseResult(DocumentContent.PlainText(textContent))

        return ParseResult(DocumentContent.Unsupported(formatType))
    }

    if (!textContent.isNullOrBlank()) return ParseResult(DocumentContent.PlainText(textContent))

    return ParseResult(DocumentContent.Empty)
}

private fun collectPlaintext(element: JsonElement, out: StringBuilder) {
    when (element) {
        is JsonObject -> {
            val text = element["plaintext"]?.jsonPrimitive?.contentOrNull
            if (!text.isNullOrBlank()) {
                if (out.isNotEmpty()) out.append("\n\n")
                out.append(text)
            }
            for ((key, child) in element) {
                if (key == "plaintext") continue
                collectPlaintext(child, out)
            }
        }
        is JsonArray -> element.forEach { collectPlaintext(it, out) }
        else -> {}
    }
}
