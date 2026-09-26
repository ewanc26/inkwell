package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BlueskyPostModelsTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `decodes enriched Standard site external embed fields`() {
        val payload = """
        {
            "uri": "https://example.com/doc",
            "title": "Standard.site Post",
            "description": "Longform post",
            "readingTime": 5,
            "createdAt": "2026-09-21T00:00:00Z",
            "associatedRefs": [
                {
                    "uri": "at://did:plc:author/site.standard.document/123",
                    "cid": "bafyabc"
                }
            ]
        }
        """

        val external = json.decodeFromString<BSkyExternal>(payload)

        assertEquals("Standard.site Post", external.title)
        assertEquals(5, external.readingTime)
        assertEquals("2026-09-21T00:00:00Z", external.createdAt)
        assertNotNull(external.associatedRefs)
        assertEquals(1, external.associatedRefs?.size)
        assertEquals("at://did:plc:author/site.standard.document/123", external.associatedRefs?.first()?.uri)
        assertEquals("bafyabc", external.associatedRefs?.first()?.cid)
    }

    /**
     * A realistic Standard.site-enriched `app.bsky.embed.external#viewExternal`
     * payload, as returned by the Bluesky AppView after the May 2026
     * Standard.site integration: title/description/uri/thumb (the
     * pre-existing fields) plus readingTime/labels/source/associatedRefs/
     * associatedProfiles (the enriched fields added by this change).
     */
    private val enrichedStandardSiteEmbedPayload = """
    {
        "uri": "https://example.press/articles/tide-charts",
        "title": "Reading the Tide Charts",
        "description": "A field guide to the numbers on the harbor board.",
        "thumb": "https://example.press/cdn/tide-charts-cover.jpg",
        "createdAt": "2026-09-18T09:00:00Z",
        "updatedAt": "2026-09-20T14:30:00Z",
        "readingTime": 6,
        "labels": [
            { "src": "did:plc:labeler", "uri": "https://example.press/articles/tide-charts", "val": "long-form", "cts": "2026-09-18T09:00:00Z" }
        ],
        "source": {
            "uri": "https://example.press",
            "icon": "https://example.press/cdn/icon.png",
            "title": "Example Press",
            "description": "Independent harbor journalism.",
            "theme": {
                "backgroundRGB": { "r": 255, "g": 255, "b": 255 },
                "foregroundRGB": { "r": 20, "g": 20, "b": 20 },
                "accentRGB": { "r": 10, "g": 90, "b": 160 },
                "accentForegroundRGB": { "r": 255, "g": 255, "b": 255 }
            }
        },
        "associatedRefs": [
            { "uri": "at://did:plc:author/site.standard.document/123", "cid": "bafyabc" }
        ],
        "associatedProfiles": [
            { "did": "did:plc:author", "handle": "author.example", "displayName": "Harbor Author", "avatar": "https://example.press/cdn/avatar.jpg" }
        ]
    }
    """

    @Test
    fun `decodes fully enriched Standard site external embed fixture`() {
        val external = json.decodeFromString<BSkyExternal>(enrichedStandardSiteEmbedPayload)

        // Pre-existing fields keep decoding.
        assertEquals("https://example.press/articles/tide-charts", external.uri)
        assertEquals("Reading the Tide Charts", external.title)
        assertEquals("A field guide to the numbers on the harbor board.", external.description)
        assertEquals("https://example.press/cdn/tide-charts-cover.jpg", external.thumb)
        assertEquals(6, external.readingTime)
        assertEquals("2026-09-18T09:00:00Z", external.createdAt)
        assertEquals("2026-09-20T14:30:00Z", external.updatedAt)
        assertEquals("at://did:plc:author/site.standard.document/123", external.associatedRefs?.first()?.uri)

        // Newly-decoded fields: labels, source, associatedProfiles.
        assertEquals("long-form", external.labels?.first()?.value)
        assertEquals("Example Press", external.source?.title)
        assertEquals("https://example.press/cdn/icon.png", external.source?.icon)
        assertEquals(10, external.source?.theme?.accentRGB?.r)
        assertEquals(90, external.source?.theme?.accentRGB?.g)
        assertEquals(160, external.source?.theme?.accentRGB?.b)
        assertEquals("author.example", external.associatedProfiles?.first()?.handle)
        assertEquals("Harbor Author", external.associatedProfiles?.first()?.displayName)
    }

    @Test
    fun `enriched embed display helpers format for rendering`() {
        val external = json.decodeFromString<BSkyExternal>(enrichedStandardSiteEmbedPayload)

        assertTrue(external.isStandardSiteEnriched())
        assertEquals("6 min read", external.readingTimeLabel())
        assertEquals(listOf("long-form"), external.contentWarningLabels())
        assertEquals("Example Press", external.sourceDisplayTitle())
    }

    @Test
    fun `plain external embed is not Standard site enriched`() {
        val payload = """
        {
            "uri": "https://example.com/story",
            "title": "A story",
            "description": "A description",
            "thumb": "https://example.com/thumb.jpg"
        }
        """
        val external = json.decodeFromString<BSkyExternal>(payload)

        assertFalse(external.isStandardSiteEnriched())
        assertNull(external.readingTimeLabel())
        assertEquals(emptyList(), external.contentWarningLabels())
        assertNull(external.source)
        assertNull(external.labels)
        assertNull(external.associatedProfiles)
    }

    @Test
    fun `unknown future fields do not break external embed decoding`() {
        val payload = """
        {
            "uri": "https://example.com/story",
            "title": "A story",
            "description": "A description",
            "futureField": { "nested": true },
            "anotherNewThing": [1, 2, 3]
        }
        """
        val external = json.decodeFromString<BSkyExternal>(payload)

        assertEquals("A story", external.title)
    }
}
