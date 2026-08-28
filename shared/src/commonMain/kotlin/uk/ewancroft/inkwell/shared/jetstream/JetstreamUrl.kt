package uk.ewancroft.inkwell.shared.jetstream

import io.ktor.http.encodeURLQueryComponent

private const val JETSTREAM_SUBSCRIBE_URL =
    "wss://jetstream.us-east.bsky.network/xrpc/network.bsky.jetstream.subscribeEvents"

/** Builds the canonical v2 Jetstream URL. Array parameters are represented by
 * repeated query keys, as required by the subscription lexicon. */
internal fun buildJetstreamUrl(config: JetstreamConfig): String {
    val params = buildList {
        config.collections.forEach { collection ->
            add("collections=${collection.encodeURLQueryComponent()}")
        }
        config.dids.forEach { did ->
            add("dids=${did.encodeURLQueryComponent()}")
        }
        add("kinds=commit")
        config.cursor?.let { cursor -> add("cursor=$cursor") }
    }
    return "$JETSTREAM_SUBSCRIBE_URL?${params.joinToString("&")}"
}
