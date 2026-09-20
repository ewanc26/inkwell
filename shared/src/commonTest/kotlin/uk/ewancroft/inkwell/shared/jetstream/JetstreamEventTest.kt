package uk.ewancroft.inkwell.shared.jetstream

import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JetstreamEventTest {
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

    @Test fun `create update and delete are valid commit operations`() {
        assertTrue(payload("create").isValidCommitOperation())
        assertTrue(payload("update").isValidCommitOperation())
        assertTrue(payload("delete").isValidCommitOperation())
    }

    @Test fun `non commit kind and unknown operation are rejected`() {
        assertFalse(payload("commit").isValidCommitOperation())
        assertFalse(payload("create", type = "network.bsky.jetstream.subscribeEvents#identity").isValidCommitOperation())
    }
}
