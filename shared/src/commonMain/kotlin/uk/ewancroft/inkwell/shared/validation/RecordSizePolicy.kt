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

    fun exceedsLimit(encodedByteCount: Int): Boolean = encodedByteCount > MAX_DOCUMENT_RECORD_BYTES
}
