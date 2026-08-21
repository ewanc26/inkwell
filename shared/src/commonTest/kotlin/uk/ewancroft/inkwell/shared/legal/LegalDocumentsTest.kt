package uk.ewancroft.inkwell.shared.legal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegalDocumentsTest {
    @Test
    fun privacyMarkdownContainsExpectedSections() {
        assertTrue(LegalDocuments.privacyMarkdown.contains("## 1. Who is responsible for your data"))
        assertTrue(LegalDocuments.privacyMarkdown.contains("## 15. Contact"))
        assertFalse(LegalDocuments.privacyMarkdown.contains("undefined"))
    }

    @Test
    fun termsMarkdownContainsExpectedSections() {
        assertTrue(LegalDocuments.termsMarkdown.contains("13 years old"))
        assertTrue(LegalDocuments.termsMarkdown.contains("## 12. AI-assisted contributions"))
        assertFalse(LegalDocuments.termsMarkdown.contains("undefined"))
    }

    @Test
    fun versionMetadataIsPresent() {
        assertTrue(LegalDocuments.VERSION.isNotBlank())
        assertTrue(LegalDocuments.EFFECTIVE_DATE.isNotBlank())
        assertTrue(LegalDocuments.privacyMarkdown.startsWith("**Version ${LegalDocuments.VERSION}"))
    }
}
