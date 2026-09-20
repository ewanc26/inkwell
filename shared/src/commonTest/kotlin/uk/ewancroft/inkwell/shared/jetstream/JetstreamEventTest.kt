package uk.ewancroft.inkwell.shared.jetstream

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class JetstreamEventTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun payload(operation: String, type: String = "network.bsky.jetstream.subscribeEvents#commit") =
        JetstreamPayload(
            type = type,
            did = "did:plc:alice",
            seq = 1,
            time = "2026-09-20T00:00:00Z",
            operation = operation,
            collection = "site.standard.document",
            rkey = "post",
            record = JsonObject(emptyMap()),
        )

    @Test fun `jetstream frames have an independent byte budget`() {
        assertTrue(isSafeJetstreamFrame(ByteArray(MAX_JETSTREAM_FRAME_BYTES)))
        assertFalse(isSafeJetstreamFrame(ByteArray(MAX_JETSTREAM_FRAME_BYTES + 1)))
        assertEquals(2 * 1024 * 1024, MAX_JETSTREAM_FRAME_BYTES)
    }

    @Test fun `create update and delete are valid commit operations`() {
        assertTrue(payload("create").isValidCommitOperation())
        assertTrue(payload("update").isValidCommitOperation())
        assertTrue(payload("delete").isValidCommitOperation())
    }

    @Test fun `non commit kind and unknown operation are rejected`() {
        assertFalse(payload("commit").isValidCommitOperation())
        assertFalse(payload("create", type = "network.bsky.jetstream.subscribeEvents#identity").isValidCommitOperation())
    }

    @Test fun `canonical commit wire fixtures preserve all operations`() {
        listOf("create", "update", "delete").forEach { operation ->
            val record = if (operation == "delete") "null" else "{\"\$type\":\"site.standard.document\"}"
            val frame = """
                {"${'$'}type":"message","payload":{"${'$'}type":"network.bsky.jetstream.subscribeEvents#commit","did":"did:plc:alice","seq":42,"time":"2026-09-20T00:00:00Z","operation":"$operation","collection":"site.standard.document","rkey":"post","record":$record,"cursor":42}}
            """.trimIndent()

            val decoded = json.decodeFromString<JetstreamEvent>(frame).payload
            assertTrue(decoded.isValidCommitOperation())
            assertTrue(decoded.operation == operation)
            assertTrue(decoded.record == null == (operation == "delete"))
        }
    }

    @Test fun `canonical non commit wire fixture is not dispatched`() {
        val frame = """
            {"${'$'}type":"message","payload":{"${'$'}type":"network.bsky.jetstream.subscribeEvents#identity","did":"did:plc:alice","seq":42,"time":"2026-09-20T00:00:00Z","operation":"create","collection":"","rkey":"","cursor":42}}
        """.trimIndent()

        val decoded = json.decodeFromString<JetstreamEvent>(frame).payload
        assertFalse(decoded.isValidCommitOperation())
    }
}
