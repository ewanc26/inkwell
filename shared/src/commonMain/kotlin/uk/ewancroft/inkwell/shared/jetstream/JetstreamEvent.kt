package uk.ewancroft.inkwell.shared.jetstream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Top-level envelope received from the Jetstream WebSocket.
 *
 * Mirrors the wire format documented at
 * https://bsky.network/docs/jetstream/ — a `$type`-tagged envelope
 * carrying a `payload` that is one of commit / identity / account / sync.
 */
@Serializable
data class JetstreamEvent(
    @SerialName("\$type") val type: String = "message",
    val payload: JetstreamPayload
)

/**
 * The payload of a Jetstream event.  For `commit` events the `record`
 * field carries the decoded AT Protocol record as a raw JSON object so
 * callers can extract the fields they need without a shared schema for
 * every possible collection.
 */
@Serializable
data class JetstreamPayload(
    @SerialName("\$type") val type: String = "network.bsky.jetstream.subscribeEvents#commit",
    val did: String,
    val seq: Long,
    val time: String,
    val operation: String,
    val collection: String,
    val rkey: String,
    val cid: String? = null,
    val record: JsonObject? = null,
    val cursor: Long? = null
)
