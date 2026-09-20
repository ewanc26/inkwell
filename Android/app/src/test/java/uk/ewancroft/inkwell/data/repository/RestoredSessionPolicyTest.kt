package uk.ewancroft.inkwell.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoredSessionPolicyTest {
    @Test fun `accepts authenticated subject matching stored DID`() {
        assertTrue(restoredSessionMatches("did:plc:alice", "did:plc:alice"))
    }

    @Test fun `rejects missing or reassigned authenticated subject`() {
        assertFalse(restoredSessionMatches("did:plc:alice", null))
        assertFalse(restoredSessionMatches("did:plc:alice", "did:plc:mallory"))
    }
}
