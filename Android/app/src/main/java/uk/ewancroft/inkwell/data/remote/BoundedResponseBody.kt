package uk.ewancroft.inkwell.data.remote

import okhttp3.ResponseBody
import okio.Buffer
import java.io.IOException

private const val MAX_JSON_RESPONSE_BYTES = 2 * 1024 * 1024L

/** Reads an untrusted JSON response without allowing an unbounded allocation. */
internal fun ResponseBody.readBoundedUtf8(maxBytes: Long = MAX_JSON_RESPONSE_BYTES): String {
    if (contentLength() > maxBytes) {
        throw IOException("Response exceeded the ${maxBytes}-byte safety budget")
    }
    require(maxBytes >= 0) { "maxBytes must not be negative" }
    val source = source()
    val buffer = Buffer()
    var total = 0L
    while (total <= maxBytes) {
        val read = source.read(buffer, minOf(16 * 1024L, maxBytes + 1 - total))
        if (read == -1L) break
        total += read
    }
    if (total > maxBytes) {
        throw IOException("Response exceeded the ${maxBytes}-byte safety budget")
    }
    return buffer.readByteArray().toString(Charsets.UTF_8)
}
