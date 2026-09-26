package uk.ewancroft.inkwell.shared.validation

import uk.ewancroft.inkwell.shared.model.DocumentMetadata

/** Field-level validation shared by the Standard.site writers. */
object StandardSiteValidation {
    data class Error(val field: String, val message: String)

    data class DocumentInput(
        val site: String,
        val title: String,
        val description: String?,
        val tags: List<String>?,
        val path: String?,
        val publishedAt: String,
    )

    fun validateDocument(input: DocumentInput): List<Error> = buildList {
        if (input.site.isBlank()) add(Error("site", "Site is required"))
        if (input.title.isBlank()) add(Error("title", "Title is required"))
        else checkText("title", input.title, 500, 5_000)
        checkOptionalText("description", input.description, 3_000, 30_000)
        input.tags.orEmpty().forEachIndexed { index, tag ->
            checkText("tags[$index]", tag, 128, 1_280)
        }
        input.path?.let { path ->
            if (path.isNotEmpty() && (!path.startsWith('/') || path.endsWith('/'))) {
                add(Error("path", "Path must start with / and not end with /"))
            }
        }
        if (input.publishedAt.isBlank()) add(Error("publishedAt", "Published date is required"))
    }

    /**
     * Validates the document's optional metadata layer.
     *
     * Kept separate from [validateDocument] so a Writer can surface a bad tag or
     * contributor against the field the user was editing rather than failing the
     * whole publish with one message. Limits come from the Lexicon: tags 128
     * graphemes / 1,280 bytes, contributor role and displayName 100 graphemes /
     * 1,000 bytes.
     */
    fun validateMetadata(metadata: DocumentMetadata): List<Error> = buildList {
        metadata.tags.forEachIndexed { index, tag ->
            checkText("tags[$index]", tag, 128, 1_280)
        }
        metadata.contributors.forEachIndexed { index, contributor ->
            if (contributor.did.isBlank()) {
                add(Error("contributors[$index].did", "Contributor DID is required"))
            } else if (!contributor.did.startsWith("did:")) {
                add(Error("contributors[$index].did", "Contributor must be a DID (did:plc:… or did:web:…)"))
            }
            checkOptionalText("contributors[$index].role", contributor.role, 100, 1_000)
            checkOptionalText("contributors[$index].displayName", contributor.displayName, 100, 1_000)
        }
        metadata.bskyPostRef?.let { ref ->
            if (!ref.uri.startsWith("at://")) {
                add(Error("bskyPostRef", "Bluesky post reference must be an at:// URI"))
            }
        }
        metadata.labels.forEachIndexed { index, label ->
            checkText("labels[$index]", label, 128, 640)
        }
    }

    fun validatePublication(url: String, name: String, description: String?): List<Error> = buildList {
        if (!url.trim().startsWith("https://", ignoreCase = true)) {
            add(Error("url", "Publication URL must use HTTPS"))
        }
        checkText("name", name, 500, 5_000)
        checkOptionalText("description", description, 3_000, 30_000)
    }

    private fun MutableList<Error>.checkOptionalText(field: String, value: String?, graphemes: Int, bytes: Int) {
        value?.let { checkText(field, it, graphemes, bytes) }
    }

    private fun MutableList<Error>.checkText(field: String, value: String, graphemes: Int, bytes: Int) {
        if (value.isEmpty()) return
        val graphemeCount = approximateGraphemeCount(value)
        if (graphemeCount > graphemes) add(Error(field, "Must be at most $graphemes characters"))
        if (value.encodeToByteArray().size > bytes) add(Error(field, "Must be at most $bytes UTF-8 bytes"))
    }

    // Common code has no ICU dependency. This handles surrogate pairs, combining marks,
    // variation selectors, emoji modifiers, and ZWJ sequences without changing authored text.
    private fun approximateGraphemeCount(value: String): Int {
        var count = 0
        var joinNext = false
        value.forEach { character ->
            val code = character.code
            val combining = code in 0x0300..0x036f || code in 0xfe00..0xfe0f || code in 0x1f3fb..0x1f3ff
            if (!combining && !joinNext) count++
            joinNext = character == '\u200d'
        }
        return count
    }
}
