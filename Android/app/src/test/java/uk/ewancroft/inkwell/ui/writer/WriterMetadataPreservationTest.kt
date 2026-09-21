package uk.ewancroft.inkwell.ui.writer

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class WriterMetadataPreservationTest {
    @Test
    fun keepsUnknownTopLevelFieldsWhenKnownFieldsAreUpdated() {
        val existing = buildJsonObject {
            put("\$type", "site.standard.document")
            put("customMetadata", "keep me")
            put("title", "old")
        }

        val merged = mergeExistingDocumentRecord(existing) { put("title", "new") }

        assertEquals("new", merged["title"]?.jsonPrimitive?.content)
        assertEquals("keep me", merged["customMetadata"]?.jsonPrimitive?.content)
    }

    @Test
    fun keepsPublishedAtAndWritesUpdatedAtWhenEditing() {
        val existing = buildJsonObject {
            put("publishedAt", "2026-01-01T00:00:00Z")
        }
        val edited = buildJsonObject {
            applyEditTimestamps(this, existing, "2026-09-21T18:00:00Z")
        }

        assertEquals("2026-01-01T00:00:00Z", edited["publishedAt"]?.jsonPrimitive?.content)
        assertEquals("2026-09-21T18:00:00Z", edited["updatedAt"]?.jsonPrimitive?.content)
    }
}
