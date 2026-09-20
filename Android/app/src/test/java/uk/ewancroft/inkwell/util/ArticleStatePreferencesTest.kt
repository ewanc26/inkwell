package uk.ewancroft.inkwell.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleStatePreferencesTest {
    @Test fun `accepts version one envelope and ignores future fields`() {
        val raw = """{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"at://did:plc:alice/site.standard.document/one","title":"Example","isRead":true,"isBookmarked":false,"timestamp":"2026-09-19T00:00:00Z","futureField":"ignored"}]}"""
        assertEquals(ArticleStatePreferences.ImportResult.Success(1), ArticleStatePreferences.previewImportJson(null, raw))
    }

    @Test fun `rejects unsupported export versions before reading local state`() {
        val raw = """{"format":"uk.ewancroft.inkwell.reading-data","version":2,"exportedAt":"2026-09-19T00:00:00Z","articles":[]}"""
        assertEquals(ArticleStatePreferences.ImportResult.UnsupportedVersion, ArticleStatePreferences.previewImportJson(null, raw))
    }

    @Test fun `rejects malformed article identifiers`() {
        val raw = """{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"https://example.com","title":"bad","isRead":false,"isBookmarked":false,"timestamp":"2026-09-19T00:00:00Z"}]}"""
        assertTrue(ArticleStatePreferences.previewImportJson(null, raw) is ArticleStatePreferences.ImportResult.Invalid)
    }

    @Test fun `rejects oversized titles and future timestamps`() {
        val oversized = """{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"at://did:plc:alice/site.standard.document/one","title":"${"x".repeat(501)}","isRead":false,"isBookmarked":false,"timestamp":"2026-09-19T00:00:00Z"}]}"""
        val future = """{"format":"uk.ewancroft.inkwell.reading-data","version":1,"exportedAt":"2026-09-19T00:00:00Z","articles":[{"articleId":"at://did:plc:alice/site.standard.document/one","title":"ok","isRead":false,"isBookmarked":false,"timestamp":"2999-01-01T00:00:00Z"}]}"""

        assertTrue(ArticleStatePreferences.previewImportJson(null, oversized) is ArticleStatePreferences.ImportResult.Invalid)
        assertTrue(ArticleStatePreferences.previewImportJson(null, future) is ArticleStatePreferences.ImportResult.Invalid)
    }
}
