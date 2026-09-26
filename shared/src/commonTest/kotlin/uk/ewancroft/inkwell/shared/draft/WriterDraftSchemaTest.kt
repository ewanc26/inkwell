package uk.ewancroft.inkwell.shared.draft

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WriterDraftSchemaTest {

    private val savedAt = "2026-09-01T12:00:00Z"
    private val savedMs = 1_788_264_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun draft(
        did: String = "did:plc:alice",
        editing: String? = null,
        revision: String? = null,
    ) = WriterDraftSchema(
        accountDid = did,
        publicationUri = "at://did:plc:alice/site.standard.publication/pub",
        editingDocumentUri = editing,
        editingDocumentRevision = revision,
        title = "Title",
        markdown = "Body ✍️",
        metadataContributors = """[{"did":"did:plc:bob"}]""",
        savedAt = savedAt,
    )

    @Test
    fun roundTripsThroughCodec() {
        val original = draft(editing = "at://did:plc:alice/site.standard.document/a", revision = "bafy1")
        assertEquals(original, WriterDraftCodec.decode(WriterDraftCodec.encode(original)))
    }

    @Test
    fun decodeRejectsMalformedAndFutureSchemas() {
        assertNull(WriterDraftCodec.decode("not json"))
        val future = WriterDraftCodec.encode(draft().copy(schemaVersion = 2))
        assertNull(WriterDraftCodec.decode(future))
    }

    @Test
    fun decodeIgnoresUnknownFields() {
        val raw = WriterDraftCodec.encode(draft()).dropLast(1) + ""","futureField":true}"""
        assertEquals(draft(), WriterDraftCodec.decode(raw))
    }

    @Test
    fun staleAfterThirtyDays() {
        assertFalse(WriterDraftPolicy.isDraftStale(savedAt, savedMs + 30 * day))
        assertTrue(WriterDraftPolicy.isDraftStale(savedAt, savedMs + 30 * day + 1))
        assertTrue(WriterDraftPolicy.isDraftStale("garbage", savedMs))
    }

    @Test
    fun restorableOnlyForOwningAccount() {
        assertTrue(WriterDraftPolicy.isRestorable(draft(), "did:plc:alice", savedMs))
        assertFalse(WriterDraftPolicy.isRestorable(draft(), "did:plc:bob", savedMs))
    }

    @Test
    fun revisionConflictOnlyForSameDocumentWithDifferentCid() {
        val uri = "at://did:plc:alice/site.standard.document/a"
        val edited = draft(editing = uri, revision = "bafy1")
        assertFalse(WriterDraftPolicy.hasRevisionConflict(edited, uri, "bafy1"))
        assertTrue(WriterDraftPolicy.hasRevisionConflict(edited, uri, "bafy2"))
        assertFalse(WriterDraftPolicy.hasRevisionConflict(edited, "at://did:plc:alice/site.standard.document/b", "bafy2"))
        assertFalse(WriterDraftPolicy.hasRevisionConflict(draft(), uri, "bafy2"))
    }

    @Test
    fun emptyDraftDetection() {
        val blank = WriterDraftSchema(accountDid = "did:plc:alice", publicationUri = "", savedAt = savedAt)
        assertTrue(WriterDraftPolicy.isEmpty(blank))
        assertFalse(WriterDraftPolicy.isEmpty(blank.copy(markdown = "x")))
        assertFalse(WriterDraftPolicy.isEmpty(blank.copy(metadataTags = listOf("t"))))
    }
}
