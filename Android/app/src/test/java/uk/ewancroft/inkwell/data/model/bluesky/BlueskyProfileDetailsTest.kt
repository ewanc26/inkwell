package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies [BlueskyProfile] decodes the `profileViewDetailed`-only
 * presentation fields (`app.bsky.actor.getProfile`): pronouns today, with
 * verification and the pinned post to follow. List subjects decode as
 * `profileView`, which carries none of these — so each must tolerate being
 * absent.
 */
class BlueskyProfileDetailsTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val fixture = """
        {
          "did": "did:plc:example",
          "handle": "example.bsky.social",
          "displayName": "Example",
          "pronouns": "they/them",
          "verification": {"verifiedStatus": "valid", "trustedVerifierStatus": "none"},
          "description": "Testing details",
          "labels": [],
          "createdAt": "2023-04-26T05:22:14.855Z",
          "indexedAt": "2026-07-22T10:12:57.160Z"
        }
    """.trimIndent()

    @Test
    fun `decodes detailed profile presentation fields`() {
        val decoded = json.decodeFromString<BlueskyProfile>(fixture)

        assertEquals("they/them", decoded.pronouns)
        assertEquals(true, decoded.isVerifiedAccount())
    }

    @Test
    fun `non valid verification status is not a verified account`() {
        val decoded = json.decodeFromString<BlueskyProfile>(
            """{"did":"did:plc:example","handle":"example.bsky.social","verification":{"verifiedStatus":"invalid"}}""",
        )

        assertEquals(false, decoded.isVerifiedAccount())
    }

    @Test
    fun `missing presentation fields decode as null for profileView subjects`() {
        val decoded = json.decodeFromString<BlueskyProfile>(
            """{"did":"did:plc:example","handle":"example.bsky.social"}""",
        )

        assertNull(decoded.pronouns)
        assertNull(decoded.verification)
        assertEquals(false, decoded.isVerifiedAccount())
    }
}
