package uk.ewancroft.inkwell.shared.text

/**
 * Shared UTF-8 byte-offset ↔ character-index conversion.
 *
 * AT Protocol facet byte ranges are UTF-8 offsets, not platform
 * character indices. Both platforms need to map these to local
 * string indices for attributed-text rendering.
 */
object Utf8Offsets {
    /**
     * Returns the UTF-8 byte length of a single character (code point).
     * Matches the byte-counting algorithm used in both platforms'
     * inline renderers.
     */
    private fun charByteLength(code: Int): Int = when {
        code < 0x80 -> 1
        code < 0x800 -> 2
        code < 0xD800 || code > 0xDFFF -> 3
        else -> 4
    }

    /**
     * Converts a UTF-8 byte range [byteStart, byteEnd) to a
     * character-index range within [text]. Returns [startChar, endChar]
     * inclusive — endChar is the character containing the byte at byteEnd.
     *
     * Returns null if the range is invalid or empty.
     */
    fun byteRangeToCharRange(text: String, byteStart: Int, byteEnd: Int): IntRange? {
        if (byteEnd <= byteStart || byteStart < 0) return null

        var startChar = -1
        var endChar = -1
        var bytePos = 0

        for (i in text.indices) {
            val charBytes = charByteLength(text[i].code)

            if (startChar == -1 && bytePos + charBytes > byteStart) {
                startChar = i
            }
            if (endChar == -1 && bytePos + charBytes > byteEnd) {
                endChar = i
            }

            bytePos += charBytes
            if (startChar != -1 && endChar != -1) break
        }

        if (startChar == -1) startChar = text.length
        if (endChar == -1) endChar = text.length
        if (bytePos < byteEnd) endChar = text.length

        if (startChar >= endChar) return null
        return startChar..endChar
    }

    /**
     * Returns the cumulative UTF-8 byte offset of a character index
     * within [text]. Equivalent to counting bytes of all characters
     * before [charIndex].
     */
    fun charIndexToByteOffset(text: String, charIndex: Int): Int {
        var bytePos = 0
        for (i in 0 until minOf(charIndex, text.length)) {
            bytePos += charByteLength(text[i].code)
        }
        return bytePos
    }

    /**
     * Returns the total UTF-8 byte length of [text].
     */
    fun byteLength(text: String): Int {
        var total = 0
        for (c in text) total += charByteLength(c.code)
        return total
    }
}
