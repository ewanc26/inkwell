package uk.ewancroft.inkwell.data.remote

import okhttp3.ResponseBody
import java.io.IOException

private const val MAX_JSON_RESPONSE_BYTES = 2 * 1024 * 1024L

/** Reads an untrusted JSON response without allowing an unbounded allocation. */
internal fun ResponseBody.readBoundedUtf8(maxBytes: Long = MAX_JSON_RESPONSE_BYTES): String {
    if (contentLength() > maxBytes) {
        throw IOException("Response exceeded the ${maxBytes}-byte safety budget")
    }
    val bytes = source().readByteArray(maxBytes + 1)
    if (bytes.size.toLong() > maxBytes) {
        throw IOException("Response exceeded the ${maxBytes}-byte safety budget")
    }
    return bytes.toString(Charsets.UTF_8)
}
