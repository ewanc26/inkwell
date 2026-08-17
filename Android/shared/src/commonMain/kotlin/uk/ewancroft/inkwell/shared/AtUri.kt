package uk.ewancroft.inkwell.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parsed AT-URI: did + collection + recordKey extracted from the
 * standard `at://did/collection/rkey` format.
 *
 * Mirrors the iOS `ATURI` struct in `StandardSiteTypes.swift` and the
 * Android `AtUri` data class in `Models.kt` — identical parse semantics.
 */
@Serializable
data class AtUri(val did: String, val collection: String, val recordKey: String) {
    companion object {
        /** Parses at:// URIs. Returns null for malformed input. */
        fun parse(uri: String): AtUri? {
            if (!uri.startsWith("at://")) return null
            val withoutScheme = uri.removePrefix("at://")
            val parts = withoutScheme.split("/", limit = 3)
            if (parts.size != 3 || parts.any { it.isEmpty() }) return null
            return AtUri(did = parts[0], collection = parts[1], recordKey = parts[2])
        }
    }

    /** Reassembles the canonical AT-URI string. */
    val uri: String get() = "at://$did/$collection/$recordKey"
}
