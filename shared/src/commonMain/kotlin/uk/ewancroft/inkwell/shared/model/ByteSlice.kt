package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a Leaflet byte slice (UTF-8 offset range).
 *
 * Mirrors Android `ByteSlice` and iOS `LeafletByteSlice`.
 */
data class ByteSlice(
    val byteStart: Int,
    val byteEnd: Int
)
