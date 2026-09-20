package uk.ewancroft.inkwell.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationMetadataTest {
    @Test
    fun `visible document keeps title and publication metadata`() {
        val notification = NewDocument(
            uri = "at://did:plc:author/site.standard.document/post",
            title = "Visible title",
            publicationName = "A publication",
            publishedAt = "2026-09-20T00:00:00Z",
        ).toNotification(date = 42L)

        assertEquals("Visible title", notification.documentTitle)
        assertEquals("A publication", notification.publicationName)
        assertEquals(42L, notification.date)
    }

    @Test
    fun `sensitive document replaces all exposed metadata`() {
        val notification = NewDocument(
            uri = "at://did:plc:author/site.standard.document/hidden",
            title = "Sensitive title",
            publicationName = "Sensitive publication",
            publishedAt = "2026-09-20T00:00:00Z",
            sensitive = true,
        ).toNotification(date = 43L)

        assertEquals("Hidden document", notification.documentTitle)
        assertNull(notification.publicationName)
        assertEquals(43L, notification.date)
    }
}
