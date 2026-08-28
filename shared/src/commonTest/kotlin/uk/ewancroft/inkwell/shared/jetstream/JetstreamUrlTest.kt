package uk.ewancroft.inkwell.shared.jetstream

import kotlin.test.Test
import kotlin.test.assertEquals

class JetstreamUrlTest {

    @Test
    fun arraysUseRepeatedEncodedQueryParameters() {
        val url = buildJetstreamUrl(
            JetstreamConfig(
                collections = listOf("site.standard.document", "site.standard.publication"),
                dids = listOf("did:plc:first", "did:web:example.com"),
                cursor = 42,
            )
        )

        assertEquals(
            "wss://jetstream.us-east.bsky.network/xrpc/network.bsky.jetstream.subscribeEvents" +
                "?collections=site.standard.document" +
                "&collections=site.standard.publication" +
                "&dids=did:plc:first" +
                "&dids=did:web:example.com" +
                "&kinds=commit&cursor=42",
            url,
        )
    }
}
