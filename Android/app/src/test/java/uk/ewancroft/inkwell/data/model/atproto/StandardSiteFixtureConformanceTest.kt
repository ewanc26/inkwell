package uk.ewancroft.inkwell.data.model.atproto

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import uk.ewancroft.inkwell.data.model.graph.GraphRecommend
import uk.ewancroft.inkwell.data.model.graph.GraphSubscription

/**
 * Decoder and encoder round-trip tests against the canonical Standard.site
 * fixtures pinned in `lexicons/standard-site/fixtures/` (see
 * `tools/lexicons/update-standard-site.mjs` for how they're pinned and
 * validated against the checked-in Lexicon schemas). iOS exercises the
 * identical JSON files through Swift's `Codable` in
 * `iOS/InkwellTests/StandardSiteFixtureConformanceTests.swift`.
 *
 * Both platforms read the *same* files rather than each hand-transcribing its
 * own copy of "what a Standard.site record looks like" -- see issue #65 -- so
 * a wire-format regression on either platform's decoder shows up here against
 * real, schema-validated data instead of only against whatever a test author
 * happened to type inline.
 */
class StandardSiteFixtureConformanceTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Locates `lexicons/standard-site/fixtures/` by walking up from the
     * Gradle test working directory, rather than copying the JSON into test
     * resources -- that would create exactly the second transcription this
     * suite exists to catch drift in.
     */
    private fun fixturesRoot(): File {
        var dir: File? = File(".").absoluteFile
        repeat(10) {
            val current = dir ?: return@repeat
            val candidate = File(current, "lexicons/standard-site/fixtures")
            if (candidate.isDirectory) return candidate
            dir = current.parentFile
        }
        throw IllegalStateException(
            "Could not locate lexicons/standard-site/fixtures above ${File(".").absolutePath}"
        )
    }

    private fun fixtureText(relativePath: String): String =
        File(fixturesRoot(), relativePath).readText()

    // ── site.standard.publication ───────────────────────────────────────

    @Test
    fun `decodes minimal publication fixture`() {
        val record = json.decodeFromString(PublicationRecord.serializer(), fixtureText("publication.minimal.json"))

        assertEquals("https://example.publication.test", record.url)
        assertEquals("A publication with only the required fields", record.name)
        assertNull(record.description)
        assertNull(record.basicTheme)
    }

    @Test
    fun `decodes full publication fixture and encoder round trips`() {
        val text = fixtureText("publication.full.json")
        val record = json.decodeFromString(PublicationRecord.serializer(), text)

        assertEquals("https://standard.site", record.url)
        assertEquals("Standard.site", record.name)
        assertEquals("image/png", record.icon?.mimeType)
        assertEquals(1696, record.icon?.size)
        assertEquals(RgbColor(r = 255, g = 255, b = 255), record.basicTheme?.background)
        assertEquals(RgbColor(r = 16, g = 16, b = 16), record.basicTheme?.foreground)
        assertEquals(RgbColor(r = 0, g = 102, b = 204), record.basicTheme?.accent)
        assertEquals(RgbColor(r = 255, g = 255, b = 255), record.basicTheme?.accentForeground)
        assertEquals(listOf("!no-unauthenticated"), record.labels?.values?.map { it.value })
        assertEquals(true, record.preferences?.showInDiscover)

        val reencoded = json.encodeToString(PublicationRecord.serializer(), record)
        val decodedAgain = json.decodeFromString(PublicationRecord.serializer(), reencoded)
        assertEquals(record, decodedAgain)
    }

    // ── site.standard.document ───────────────────────────────────────────

    @Test
    fun `decodes minimal document fixture`() {
        val record = json.decodeFromString(DocumentRecord.serializer(), fixtureText("document.minimal.json"))

        assertEquals("https://example.publication.test", record.site)
        assertEquals("A loose document with only the required fields", record.title)
        assertNull(record.path)
        assertNull(record.contributors)
    }

    @Test
    fun `decodes full document fixture and encoder round trips`() {
        val text = fixtureText("document.full.json")
        val record = json.decodeFromString(DocumentRecord.serializer(), text)

        assertEquals(
            "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.publication/3me5vykp6lf2y",
            record.site,
        )
        assertEquals("Every field Inkwell claims to preserve", record.title)
        assertEquals("/conformance/document-full", record.path)
        assertEquals(
            "Every field Inkwell claims to preserve.\n\nPlaintext has no markdown.",
            record.textContent,
        )
        assertEquals(listOf("conformance", "lexicon", "standard.site"), record.tags)
        assertEquals("image/png", record.coverImage?.mimeType)
        assertEquals(113_475, record.coverImage?.size)
        assertNotNull(record.content) // Open union (Leaflet linearDocument).
        assertNotNull(record.links) // Open union; kept as a raw JsonElement.
        assertEquals(listOf("!no-unauthenticated"), record.labels?.values?.map { it.value })
        assertEquals(2, record.contributors?.size)
        assertEquals("did:plc:re3ebnp5v7ffagz6rb6xfei4", record.contributors?.first()?.did)
        assertEquals("editor", record.contributors?.first()?.role)
        assertEquals("Standard.site", record.contributors?.first()?.displayName)
        assertEquals("did:plc:z72i7hdynmk6r22z27h6tvur", record.contributors?.last()?.did)
        assertNull(record.contributors?.last()?.role)
        assertEquals(
            "at://did:plc:z72i7hdynmk6r22z27h6tvur/app.bsky.feed.post/3lbvbzbpt2k2p",
            record.bskyPostRef?.uri,
        )
        assertNotNull(record.updatedAt)

        val reencoded = json.encodeToString(DocumentRecord.serializer(), record)
        val decodedAgain = json.decodeFromString(DocumentRecord.serializer(), reencoded)
        assertEquals(record, decodedAgain)
    }

    /**
     * `document.extensions.json` carries `theme`, `preferences`, and
     * `canonicalUrl` -- none declared by the pinned `site.standard.document`
     * Lexicon (see `lexicons/standard-site/inkwell-extensions.json`). Android
     * models both `theme` and `preferences` (unlike iOS, which only models
     * `theme`), but not `canonicalUrl` -- so a typed decode is expected to
     * keep the first two and silently drop the last, matching the asymmetry
     * the extensions file documents rather than a bug this test should flag.
     */
    @Test
    fun `decodes document extensions fixture keeping modelled fields only`() {
        val text = fixtureText("document.extensions.json")
        val record = json.decodeFromString(DocumentRecord.serializer(), text)

        assertEquals("Recommend Lexicon", record.title)
        assertNotNull("theme is modelled by Android -- see inkwell-extensions.json", record.theme)
        assertEquals(ColorValue(r = 255, g = 250, b = 240), record.theme?.backgroundColor)
        assertNotNull("preferences is modelled by Android -- see inkwell-extensions.json", record.preferences)
        assertEquals(true, record.preferences?.showComments)
        assertEquals(false, record.preferences?.showMentions)
        assertEquals(true, record.preferences?.showRecommends)
        assertEquals(true, record.preferences?.showPrevNext)
        assertEquals(true, record.preferences?.showInDiscover)

        // canonicalUrl isn't in DocumentRecord's properties at all, so there's
        // no `record.canonicalUrl` to assert against. The raw JSON still has
        // it, though:
        val raw = Json.parseToJsonElement(text).jsonObject
        assertNotNull(raw["canonicalUrl"])
    }

    // ── site.standard.graph.subscription / site.standard.graph.recommend ──

    @Test
    fun `decodes graph subscription fixture and encoder round trips`() {
        val record = json.decodeFromString(GraphSubscription.serializer(), fixtureText("graph.subscription.json"))

        assertEquals(
            "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.publication/3me5vykp6lf2y",
            record.publication,
        )
        assertNotNull(record.createdAt)

        val reencoded = json.encodeToString(GraphSubscription.serializer(), record)
        val decodedAgain = json.decodeFromString(GraphSubscription.serializer(), reencoded)
        assertEquals(record, decodedAgain)
    }

    @Test
    fun `decodes graph recommend fixture and encoder round trips`() {
        val record = json.decodeFromString(GraphRecommend.serializer(), fixtureText("graph.recommend.json"))

        assertEquals(
            "at://did:plc:re3ebnp5v7ffagz6rb6xfei4/site.standard.document/3me5vykp6lf2z",
            record.document,
        )
        assertNotNull(record.createdAt)

        val reencoded = json.encodeToString(GraphRecommend.serializer(), record)
        val decodedAgain = json.decodeFromString(GraphRecommend.serializer(), reencoded)
        assertEquals(record, decodedAgain)
    }

    // ── site.standard.theme.basic ────────────────────────────────────────

    @Test
    fun `decodes theme basic fixture and encoder round trips`() {
        val record = json.decodeFromString(BasicTheme.serializer(), fixtureText("theme.basic.json"))

        assertEquals(RgbColor(r = 14, g = 14, b = 18), record.background)
        assertEquals(RgbColor(r = 236, g = 236, b = 238), record.foreground)
        assertEquals(RgbColor(r = 120, g = 180, b = 255), record.accent)
        assertEquals(RgbColor(r = 0, g = 0, b = 0), record.accentForeground)

        val reencoded = json.encodeToString(BasicTheme.serializer(), record)
        val decodedAgain = json.decodeFromString(BasicTheme.serializer(), reencoded)
        assertEquals(record, decodedAgain)
    }

    // ── site.standard.auth{Full,Social} (derived permission-set fixture) ──

    /**
     * `auth.permission-sets.json` is derived by
     * `tools/lexicons/update-standard-site.mjs` straight from the pinned
     * authFull/authSocial permission-set Lexicons, not hand-authored -- so
     * this asserts Android's own OAuth scope/collection expectations against
     * what Standard.site's Lexicons actually grant, not against a second
     * hand-transcription of them.
     */
    @Test
    fun `auth permission sets fixture matches known scope grants`() {
        val raw = Json.parseToJsonElement(fixtureText("auth.permission-sets.json")).jsonObject
        val permissionSets = raw["permissionSets"]!!.jsonObject

        val authFull = permissionSets["site.standard.authFull"]!!.jsonObject
        val authFullCollections = authFull["collections"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(
            setOf(
                "site.standard.document",
                "site.standard.graph.recommend",
                "site.standard.graph.subscription",
                "site.standard.publication",
            ),
            authFullCollections,
        )

        val authSocial = permissionSets["site.standard.authSocial"]!!.jsonObject
        val authSocialCollections = authSocial["collections"]!!.jsonArray.map { it.jsonPrimitive.content }.toSet()
        assertEquals(
            setOf("site.standard.graph.recommend", "site.standard.graph.subscription"),
            authSocialCollections,
        )
    }

    // ── Future-field preservation regression (issue #65) ─────────────────

    /**
     * `fixtures/regression/document.future-field.json` carries a property
     * (`futureField`) that no pinned Lexicon declares -- deliberately, as a
     * stand-in for whatever Standard.site adds next. It lives outside
     * `fixtures/` (which `update-standard-site.mjs`'s conformance check
     * scans) precisely because no schema will ever legitimately declare it.
     *
     * Unlike iOS's typed `DocumentRecord` decode (which drops undeclared
     * fields before any merge logic can run -- see
     * `StandardSiteFixtureConformanceTests.swift`'s
     * `testEditingThroughTheRegisteredDecoderDropsUndeclaredFields`), the
     * Writer's real edit flow on Android never decodes into a typed
     * `DocumentRecord` at all: `loadDocumentForEditing` in
     * `WriterDocumentEditingExtensions.kt` keeps the fetched record as a raw
     * `JsonObject` (`editingDocumentRecord`), and `publish()` in
     * `WriterPublishExtensions.kt` merges edits onto that raw object via
     * `mergeExistingDocumentRecord` -- so this test exercises those actual
     * production functions, not a reimplemented stand-in for them.
     */
    @Test
    fun `mergeExistingDocumentRecord keeps a field no Lexicon declares`() {
        val existingText = fixtureText("regression/document.future-field.json")
        val existing = Json.parseToJsonElement(existingText).jsonObject

        // Simulate what publish() actually builds for an edit: it starts from
        // the raw existing record and layers only the fields a client that
        // has never heard of `futureField` knows how to write.
        val merged = uk.ewancroft.inkwell.ui.writer.mergeExistingDocumentRecord(existing) {
            put("\$type", "site.standard.document")
            put("title", "Edited title")
            uk.ewancroft.inkwell.ui.writer.applyEditTimestamps(this, existing, "2026-09-26T00:00:00.000Z")
        }

        assertEquals("Edited title", merged["title"]?.jsonPrimitive?.content)
        assertEquals(
            "This value must survive an edit round-trip even though no client models it yet.",
            merged["futureField"]?.jsonPrimitive?.content,
        )
        assertNotNull("Even a stray non-Lexicon key should survive untouched.", merged["\$comment"])

        // ensureDocumentRecordFits must accept the merged record unchanged --
        // it's a size guard, not a field filter.
        uk.ewancroft.inkwell.ui.writer.ensureDocumentRecordFits(merged)
    }
}
