package uk.ewancroft.inkwell.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Wire-contract coverage for the FCM registration hand-off (issue #152).
 *
 * The registration endpoint validates `platform`/`deviceToken`/`topics`/
 * `did` by shape and rejects anything else, so this pins the exact JSON
 * [buildPushRegistrationBody] produces without needing Firebase.
 */
class PushRegistrationBodyTest {

    @Test
    fun `registration body matches the push register wire contract`() {
        assertEquals(
            "{\"platform\":\"android\"," +
                "\"deviceToken\":\"fcm-token-123\"," +
                "\"topics\":[\"subscribe\",\"recommend\",\"comment\"]," +
                "\"did\":\"did:plc:alice\"}",
            buildPushRegistrationBody("fcm-token-123", "did:plc:alice"),
        )
    }

    @Test
    fun `registration body escapes quotes in token and did`() {
        val body = buildPushRegistrationBody("tok\"en", "did:plc:\"x\"")
        assertTrue(body.contains("\"deviceToken\":\"tok\\\"en\""))
        assertTrue(body.contains("\"did\":\"did:plc:\\\"x\\\"\""))
    }
}
