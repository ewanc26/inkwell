package uk.ewancroft.inkwell.shared.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StandardSiteValidationTest {
    @Test
    fun enforcesUtf8BytesSeparatelyFromGraphemes() {
        val errors = StandardSiteValidation.validatePublication("https://example.com", ("e\u0301".repeat(500)).replace("\u0301", "\u0301".repeat(11)), null)
        assertTrue(errors.any { it.field == "name" && it.message.contains("UTF-8") })
        assertTrue(errors.none { it.field == "name" && it.message.contains("characters") })
    }

    @Test
    fun treatsCombiningSequenceAsOneAuthoredCharacter() {
        val value = "e\u0301".repeat(500)
        assertEquals(emptyList(), StandardSiteValidation.validatePublication("https://example.com", value, null))
    }

    @Test
    fun reportsExactDocumentFields() {
        val errors = StandardSiteValidation.validateDocument(
            StandardSiteValidation.DocumentInput("", "", "x".repeat(30_001), listOf("x".repeat(1_281)), "bad/", ""),
        )
        assertEquals(setOf("site", "title", "description", "tags[0]", "path", "publishedAt"), errors.map { it.field }.toSet())
    }
}
