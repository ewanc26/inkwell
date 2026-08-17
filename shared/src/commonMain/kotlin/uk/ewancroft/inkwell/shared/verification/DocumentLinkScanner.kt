package uk.ewancroft.inkwell.shared.verification

object DocumentLinkScanner {

    private const val DOCUMENT_LINK_REL = "site.standard.document"

    /**
     * Regex-based `<link>` search, tolerant of either attribute order and quote style.
     * Mirrors the iOS and Android implementations' approach rather than pulling in
     * an HTML parser.
     */
    fun containsDocumentLink(html: String, documentURI: String): Boolean {
        val escapedURI = Regex.escape(documentURI)
        val escapedRel = Regex.escape(DOCUMENT_LINK_REL)
        val patterns = listOf(
            "<link\\b[^>]*\\brel\\s*=\\s*[\"']$escapedRel[\"'][^>]*\\bhref\\s*=\\s*[\"']$escapedURI[\"'][^>]*>",
            "<link\\b[^>]*\\bhref\\s*=\\s*[\"']$escapedURI[\"'][^>]*\\brel\\s*=\\s*[\"']$escapedRel[\"'][^>]*>",
        )
        return patterns.any { pattern -> Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(html) }
    }
}
