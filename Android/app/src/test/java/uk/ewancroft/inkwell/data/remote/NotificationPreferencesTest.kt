package uk.ewancroft.inkwell.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class NotificationPreferencesTest {
    @Test
    fun `account namespaces are deterministic and distinct`() {
        assertEquals(
            "inkwell_notifications_did_plc_alice",
            notificationPreferencesName("did:plc:alice")
        )
        assertNotEquals(
            notificationPreferencesName("did:plc:alice"),
            notificationPreferencesName("did:plc:bob")
        )
    }
}
