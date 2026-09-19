package uk.ewancroft.inkwell.shared.validation

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
        checkText("title", input.title, 500, 5_000)
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
