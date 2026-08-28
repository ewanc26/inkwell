package uk.ewancroft.inkwell.shared.jetstream

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext

/**
 * iOS implementation of [JetstreamClient] using Ktor + Darwin.
 *
 * Each call to [connect] opens a new WebSocket session and returns a
 * cold [Flow] that emits [JetstreamPayload] objects.  The session is
 * closed when the flow is cancelled or the server disconnects.
 */
class JetstreamClientIos : uk.ewancroft.inkwell.shared.jetstream.JetstreamClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient(Darwin) {
        install(WebSockets)
        install(ContentNegotiation) {
            json(json)
        }
    }

    private var session: io.ktor.websocket.WebSocketSession? = null

    override fun connect(config: JetstreamConfig): Flow<JetstreamPayload> = flow {
        var wsSession: io.ktor.websocket.WebSocketSession? = null
        try {
            wsSession = httpClient.webSocketSession(urlString = buildJetstreamUrl(config))
            session = wsSession
            while (coroutineContext.isActive) {
                val frame = wsSession.incoming.receive()
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        try {
                            val event = json.decodeFromString<JetstreamEvent>(text)
                            if (event.payload.operation == "commit") {
                                emit(event.payload)
                            }
                        } catch (_: Exception) {
                            // Skip malformed frames
                        }
                    }
                    is Frame.Close -> break
                    else -> { /* ignore binary, ping, pong */ }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // A rejected upgrade or disconnected server ends this cold flow
            // normally. Native callers cannot safely receive arbitrary Kotlin
            // exceptions through an exported Flow completion callback.
        } finally {
            if (session === wsSession) session = null
            wsSession?.close()
        }
    }

    override suspend fun disconnect() {
        session?.close()
        session = null
    }

    override val isConnected: Boolean
        get() = session?.isActive == true

}
