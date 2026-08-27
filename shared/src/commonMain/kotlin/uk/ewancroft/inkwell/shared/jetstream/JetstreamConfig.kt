package uk.ewancroft.inkwell.shared.jetstream

/**
 * Configuration for a Jetstream WebSocket subscription.
 *
 * @param collections AT Protocol collection NSIDs to filter on
 *   (e.g. `site.standard.document`).
 * @param dids DIDs to restrict the stream to.  When non-empty only
 *   records owned by these DIDs are delivered.
 * @param cursor Optional sequence number to resume from after a
 *   disconnect.  When `null` the stream starts from the current tip.
 */
data class JetstreamConfig(
    val collections: List<String>,
    val dids: List<String>,
    val cursor: Long? = null
)
