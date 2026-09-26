package uk.ewancroft.inkwell.ui.writer

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WriterMetadataModelsTest {
    private val coverBlob = buildJsonObject {
        put("\$type", "blob")
        putJsonObject("ref") { put("\$link", "bafkreicover") }
        put("mimeType", "image/jpeg")
        put("size", 123_456)
    }

    private val existingRecord = buildJsonObject {
        put("\$type", "site.standard.document")
        put("title", "Old")
        put("customField", "keep me")
        put("tags", buildJsonArray { add(kotlinx.serialization.json.JsonPrimitive("old")) })
        // An unrecognised links variant with a non-string field the model would not re-shape.
        put("links", buildJsonArray {
            add(buildJsonObject {
                put("\$type", "com.example.link#thing")
                put("weight", 3)
            })
        })
    }

    @Test
    fun editKeepsUnknownFieldsAndLinksVerbatim() {
        val draft = WriterMetadataDraft.fromRecord(existingRecord).copy(tags = listOf("new"))

        val record = applyDocumentMetadata(existingRecord, draft)

        assertEquals("keep me", record["customField"]?.jsonPrimitive?.content)
        assertEquals(existingRecord["links"], record["links"])
        assertEquals(listOf("new"), record["tags"]?.jsonArray?.map { it.jsonPrimitive.content })
    }

    @Test
    fun clearedMetadataRemovesItsKeysInsteadOfWritingEmptyContainers() {
        val withEverything = applyDocumentMetadata(
            existingRecord,
            WriterMetadataDraft(
                tags = listOf("a"),
                contributors = listOf(WriterContributorDraft("did:plc:alice")),
                labels = listOf("nudity"),
                coverImage = coverBlob,
                bskyPostUri = "at://did:plc:alice/app.bsky.feed.post/1",
                bskyPostCid = "bafyreipost",
            ),
        )

        val cleared = applyDocumentMetadata(withEverything, WriterMetadataDraft())

        listOf("tags", "contributors", "labels", "coverImage", "bskyPostRef").forEach {
            assertFalse(cleared.containsKey(it), "$it should be removed")
        }
        assertEquals("keep me", cleared["customField"]?.jsonPrimitive?.content)
    }

    @Test
    fun writesCanonicalWireShapes() {
        val record = applyDocumentMetadata(
            JsonObject(emptyMap()),
            WriterMetadataDraft(
                contributors = listOf(WriterContributorDraft("did:plc:alice", role = "Editor")),
                labels = listOf("graphic-media"),
                coverImage = coverBlob,
                bskyPostUri = "at://did:plc:alice/app.bsky.feed.post/1",
                bskyPostCid = "bafyreipost",
            ),
        )

        val contributor = (record["contributors"] as JsonArray).single().jsonObject
        assertEquals("site.standard.document#contributor", contributor["\$type"]?.jsonPrimitive?.content)
        assertEquals("Editor", contributor["role"]?.jsonPrimitive?.content)
        val labels = record["labels"]!!.jsonObject
        assertEquals("com.atproto.label.defs#selfLabels", labels["\$type"]?.jsonPrimitive?.content)
        assertEquals(coverBlob, record["coverImage"])
        assertEquals("bafyreipost", record["bskyPostRef"]!!.jsonObject["cid"]?.jsonPrimitive?.content)
    }

    @Test
    fun fromRecordReadsTheEditableFields() {
        val record = applyDocumentMetadata(
            existingRecord,
            WriterMetadataDraft(
                tags = listOf("kotlin"),
                contributors = listOf(WriterContributorDraft("did:plc:bob", displayName = "Bob")),
                labels = listOf("sexual", "custom-label"),
                coverImage = coverBlob,
                bskyPostUri = "at://did:plc:bob/app.bsky.feed.post/2",
                bskyPostCid = "bafyreipost",
            ),
        )

        val draft = WriterMetadataDraft.fromRecord(record)

        assertEquals(listOf("kotlin"), draft.tags)
        assertEquals(listOf(WriterContributorDraft("did:plc:bob", displayName = "Bob")), draft.contributors)
        assertEquals(listOf("sexual", "custom-label"), draft.labels)
        assertEquals(coverBlob, draft.coverImage)
        assertEquals("bafyreipost", draft.bskyPostCid)
    }

    @Test
    fun draftSurvivesItsSavedStateEncoding() {
        val draft = WriterMetadataDraft(
            tags = listOf("a"),
            contributors = listOf(WriterContributorDraft("did:plc:alice", "Editor", "Alice")),
            labels = listOf("porn"),
            coverImage = coverBlob,
            bskyPostUri = "at://did:plc:alice/app.bsky.feed.post/1",
        )

        val encoded = Json.encodeToString(WriterMetadataDraft.serializer(), draft)

        assertEquals(draft, Json.decodeFromString(WriterMetadataDraft.serializer(), encoded))
    }

    @Test
    fun resolvesAMissingCid() = runTest {
        val draft = WriterMetadataDraft(bskyPostUri = " at://did:plc:a/app.bsky.feed.post/1 ")

        val resolved = resolveBskyPostRef(draft) { "bafyreiresolved" }

        assertEquals("at://did:plc:a/app.bsky.feed.post/1", resolved.bskyPostUri)
        assertEquals("bafyreiresolved", resolved.bskyPostCid)
    }

    @Test
    fun keepsAKnownCidWithoutFetching() = runTest {
        val draft = WriterMetadataDraft(bskyPostUri = "at://did:plc:a/app.bsky.feed.post/1", bskyPostCid = "known")

        val resolved = resolveBskyPostRef(draft) { error("should not fetch") }

        assertEquals("known", resolved.bskyPostCid)
    }

    @Test
    fun anUnresolvablePostIsAnErrorNotAHalfRef() = runTest {
        val draft = WriterMetadataDraft(bskyPostUri = "at://did:plc:a/app.bsky.feed.post/missing")

        assertFailsWith<WriterMetadataException> { resolveBskyPostRef(draft) { null } }
    }

    @Test
    fun aBlankUriClearsTheRef() = runTest {
        val resolved = resolveBskyPostRef(WriterMetadataDraft(bskyPostUri = "  ", bskyPostCid = "stale")) { null }

        assertEquals("", resolved.bskyPostUri)
        assertNull(resolved.bskyPostCid)
    }
}
