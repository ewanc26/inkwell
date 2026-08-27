package uk.ewancroft.inkwell.shared.jetstream

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic interface for a Jetstream WebSocket client.
 *
 * The actual implementation lives in `androidMain` (Ktor + OkHttp) and
 * `iosMain` (Ktor + Darwin).  commonMain holds only the interface and
 * the `JetstreamConfig` / `JetstreamPayload` models so business logic
 * can consume the stream without importing a platform engine.
 */
interface JetstreamClient {

    /**
     * Opens a WebSocket connection to Jetstream and returns a cold
     * [Flow] of commit payloads.  The flow completes when the
     * connection is closed by the server or cancelled by the caller.
     *
     * Only `commit` events with `operation == "create"` or
     * `operation == "update"` are emitted — deletes are mapped to a
     * sentinel with `operation == "delete"` so callers can evict stale
     * entries from their cache.
     */
    fun connect(config: JetstreamConfig): Flow<JetstreamPayload>

    /** Gracefully close the current connection, if any. */
    suspend fun disconnect()

    /** `true` while a WebSocket session is open. */
    val isConnected: Boolean
}
