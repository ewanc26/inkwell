package uk.ewancroft.inkwell.shared.validation

/**
 * Conservative ceiling for an encoded AT Protocol record.
 *
 * The protocol treats 1 MiB (1,048,576 bytes) as the practical maximum and
 * the sync protocol enforces a hard 1,000,000-byte record-block limit in
 * commit events (see https://atproto.com/specs/sync). Inkwell leaves
 * headroom below that for record metadata/CBOR-vs-JSON encoding overhead.
 */
object RecordSizePolicy {
    const val MAX_DOCUMENT_RECORD_BYTES: Int = 900 * 1024

    /**
     * Budget for a document record's portable `textContent`.
     *
     * `textContent` is an indexing/search convenience, not the document's
     * canonical content: when authored content spills into a format's
     * blob-backed representation (markpub `textBlob`, Leaflet `blobPages`)
     * the plaintext copy must not be the thing keeping the record oversized.
     * Capping it keeps the field useful — the opening of a document is what
     * search reads — without letting it dominate the record.
     */
    const val MAX_INLINE_TEXT_CONTENT_BYTES: Int = 64 * 1024

    /** Marks a truncated `textContent` so consumers don't read it as complete. */
    const val TEXT_CONTENT_TRUNCATION_SUFFIX: String = "…"

    fun exceedsLimit(encodedByteCount: Int): Boolean = encodedByteCount > MAX_DOCUMENT_RECORD_BYTES

    /**
     * Truncates [text] to at most [limit] UTF-8 bytes without splitting a
     * Unicode scalar, appending [TEXT_CONTENT_TRUNCATION_SUFFIX] when the
     * value was shortened. Returns the input unchanged when it already fits,
     * and null when nothing meaningful survives the budget.
     */
    fun truncateTextContent(
        text: String?,
        limit: Int = MAX_INLINE_TEXT_CONTENT_BYTES,
    ): String? {
        if (text == null) return null
        if (limit <= 0) return null
        if (text.encodeToByteArray().size <= limit) return text

        val suffixBytes = TEXT_CONTENT_TRUNCATION_SUFFIX.encodeToByteArray().size
        val budget = limit - suffixBytes
        if (budget <= 0) return null

        // Walk whole Unicode scalars: a surrogate pair is one scalar and its
        // halves must never be measured or emitted separately.
        var index = 0
        var used = 0
        val builder = StringBuilder()
        while (index < text.length) {
            val isSurrogatePair = text[index].isHighSurrogate() &&
                index + 1 < text.length &&
                text[index + 1].isLowSurrogate()
            val end = if (isSurrogatePair) index + 2 else index + 1
            val scalar = text.substring(index, end)
            val scalarBytes = scalar.encodeToByteArray().size
            if (used + scalarBytes > budget) break
            used += scalarBytes
            builder.append(scalar)
            index = end
        }
        if (builder.isEmpty()) return null
        return builder.toString() + TEXT_CONTENT_TRUNCATION_SUFFIX
    }
}
