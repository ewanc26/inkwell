package uk.ewancroft.inkwell.shared.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class NotificationPolicyTest {

    // ── isFirstPoll ─────────────────────────────────────────────────────

    @Test
    fun firstPollWhenNeverPolled() {
        assertTrue(NotificationPolicy.isFirstPoll(-1L))
    }

    @Test
    fun notFirstPollAfterPoll() {
        assertFalse(NotificationPolicy.isFirstPoll(1_000_000_000_000L))
    }

    // ── notificationStyle ───────────────────────────────────────────────

    @Test
    fun zeroNewDocsReturnsNone() {
        assertEquals(NotificationStyle.None, NotificationPolicy.notificationStyle(0))
    }

    @Test
    fun singleNewDocReturnsSingle() {
        assertEquals(NotificationStyle.Single, NotificationPolicy.notificationStyle(1))
    }

    @Test
    fun multipleNewDocsReturnSummary() {
        val style = NotificationPolicy.notificationStyle(3)
        assertTrue(style is NotificationStyle.Summary)
        assertEquals(3, (style as NotificationStyle.Summary).count)
    }

    @Test
    fun exactlyAtThresholdReturnsSummary() {
        val style = NotificationPolicy.notificationStyle(NotificationPolicy.SUMMARY_THRESHOLD)
        assertTrue(style is NotificationStyle.Summary)
    }

    // ── trimSeenUris ───────────────────────────────────────────────────

    @Test
    fun trimSeenUrisKeepsAllWhenUnderLimit() {
        val uris = List(100) { "uri-$it" }
        assertEquals(uris, NotificationPolicy.trimSeenUris(uris))
    }

    @Test
    fun trimSeenUrisTrimsWhenOverLimit() {
        val uris = List(600) { "uri-$it" }
        val trimmed = NotificationPolicy.trimSeenUris(uris)
        assertEquals(NotificationPolicy.MAX_SEEN_URIS, trimmed.size)
        assertEquals("uri-100", trimmed.first())
        assertEquals("uri-599", trimmed.last())
    }

    @Test
    fun trimSeenUrisAtExactLimit() {
        val uris = List(NotificationPolicy.MAX_SEEN_URIS) { "uri-$it" }
        assertEquals(NotificationPolicy.MAX_SEEN_URIS, NotificationPolicy.trimSeenUris(uris).size)
    }

    // ── trimNotifications ──────────────────────────────────────────────

    @Test
    fun trimNotificationsKeepsAllWhenUnderLimit() {
        val notifications = List(10) { mapOf("id" to it) }
        assertEquals(10, NotificationPolicy.trimNotifications(notifications).size)
    }

    @Test
    fun trimNotificationsTrimsWhenOverLimit() {
        val notifications = List(60) { mapOf("id" to it) }
        val trimmed = NotificationPolicy.trimNotifications(notifications)
        assertEquals(NotificationPolicy.MAX_NOTIFICATIONS, trimmed.size)
    }
}
