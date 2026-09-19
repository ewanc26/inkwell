package uk.ewancroft.inkwell.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleStatePreferencesTest {
    @Test fun `rejects unsupported export versions before reading local state`() {
        val raw = """{"format":"uk.ewancroft.inkwell.reading-data","version":2,"exportedAt":"2026-09-19T00:00:00Z","articles":[]}"""
        assertEquals(ArticleStatePreferences.ImportResult.UnsupportedVersion, ArticleStatePreferences.previewImportJson(null, raw))
    }

    @Test fun `rejects malformed article identifiers`() {
        val raw = """{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"https://example.com","title":"bad","isRead":false,"isBookmarked":false,"timestamp":"2026-09-19T00:00:00Z"}]}"""
        assertTrue(ArticleStatePreferences.previewImportJson(null, raw) is ArticleStatePreferences.ImportResult.Invalid)
    }
}
