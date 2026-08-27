package uk.ewancroft.inkwell.shared.jetstream

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/** JVM implementation of [JetstreamClient] using Ktor + CIO. */
class JetstreamClientJvm : JetstreamClient {

    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
        install(ContentNegotiation) { json(json) }
    }
    private var session: io.ktor.websocket.WebSocketSession? = null

    override fun connect(config: JetstreamConfig): Flow<JetstreamPayload> = callbackFlow {
        val url = buildUrl(config)
        val wsSession = httpClient.webSocketSession(urlString = url)
        session = wsSession
        try {
            while (coroutineContext.isActive) {
                when (val frame = wsSession.incoming.receive()) {
                    is Frame.Text -> try {
                        val event = json.decodeFromString<JetstreamEvent>(frame.readText())
                        if (event.payload.operation == "commit") trySend(event.payload)
                    } catch (_: Exception) { }
                    is Frame.Close -> break
                    else -> Unit
                }
            }
        } finally {
            session = null
            wsSession.close()
        }
        awaitClose { session = null }
    }

    override suspend fun disconnect() {
        session?.close()
        session = null
    }

    override val isConnected: Boolean get() = session?.isActive == true

    private fun buildUrl(config: JetstreamConfig): String {
        val base = "wss://jetstream.us-east.bsky.network/xrpc/network.bsky.jetstream.subscribeEvents"
        val params = mutableListOf<String>()
        config.collections.forEach { params.add("collections=$it") }
        if (config.dids.isNotEmpty()) params.add("dids=${config.dids.joinToString(",")}")
        params.add("kinds=commit")
        config.cursor?.let { params.add("cursor=$it") }
        return "$base?${params.joinToString("&")}"
    }
}
