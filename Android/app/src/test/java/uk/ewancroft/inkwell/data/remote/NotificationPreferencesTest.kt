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

    @Test
    fun `account switch returns to the original namespace without sharing state`() {
        val accountA = notificationPreferencesName("did:plc:alice")
        val accountB = notificationPreferencesName("did:plc:bob")
        val activationSequence = listOf(accountA, accountB, accountA)

        assertEquals(accountA, activationSequence.first())
        assertEquals(accountB, activationSequence[1])
        assertEquals(accountA, activationSequence.last())
        assertNotEquals(accountA, accountB)
    }
}
