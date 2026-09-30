package uk.ewancroft.inkwell.shared.model

import uk.ewancroft.inkwell.shared.policy.NotificationTopic
import kotlin.test.Test
import kotlin.test.assertEquals

class PushModelsTest {

    // ── PushPlatform ─────────────────────────────────────────────────────

    @Test
    fun platformWireValuesAreStable() {
        assertEquals("ios", PushPlatform.IOS.wireValue)
        assertEquals("android", PushPlatform.ANDROID.wireValue)
    }

    // ── NotificationTopic ────────────────────────────────────────────────

    @Test
    fun topicWireValuesAreStable() {
        assertEquals("subscribe", NotificationTopic.SUBSCRIBE.wireValue)
        assertEquals("recommend", NotificationTopic.RECOMMEND.wireValue)
        assertEquals("comment", NotificationTopic.COMMENT.wireValue)
    }

    // ── PushSubscriptionRequest ──────────────────────────────────────────

    @Test
    fun subscriptionRequestRetainsAllFields() {
        val request = PushSubscriptionRequest(
            platform = PushPlatform.IOS,
            deviceToken = "abc123",
            topics = listOf(NotificationTopic.SUBSCRIBE, NotificationTopic.COMMENT),
            did = "did:plc:abc123",
        )
        assertEquals(PushPlatform.IOS, request.platform)
        assertEquals("abc123", request.deviceToken)
        assertEquals(listOf(NotificationTopic.SUBSCRIBE, NotificationTopic.COMMENT), request.topics)
        assertEquals("did:plc:abc123", request.did)
    }

    // ── PushNotificationType ─────────────────────────────────────────────

    @Test
    fun notificationTypeWireValuesAreStable() {
        assertEquals("newDocument", PushNotificationType.NEW_DOCUMENT.wireValue)
        assertEquals("newComment", PushNotificationType.NEW_COMMENT.wireValue)
        assertEquals("newRecommend", PushNotificationType.NEW_RECOMMEND.wireValue)
    }

    // ── PushPayload ──────────────────────────────────────────────────────

    @Test
    fun payloadRetainsAllFields() {
        val payload = PushPayload(
            type = PushNotificationType.NEW_DOCUMENT,
            subjectUri = "at://did:plc:abc123/site.standard.document/xyz",
            actorDid = "did:plc:abc123",
            timestampEpochMillis = 1_000_000_000_000L,
        )
        assertEquals(PushNotificationType.NEW_DOCUMENT, payload.type)
        assertEquals("at://did:plc:abc123/site.standard.document/xyz", payload.subjectUri)
        assertEquals("did:plc:abc123", payload.actorDid)
        assertEquals(1_000_000_000_000L, payload.timestampEpochMillis)
    }
}
