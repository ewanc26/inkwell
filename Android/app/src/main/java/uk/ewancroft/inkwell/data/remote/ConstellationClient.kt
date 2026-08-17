/**
 * Queries the microcosm.blue Constellation API — a global AT Protocol backlink
 * index — for cross-repo discovery of comments, recommends, and mentions.
 *
 * Without this, records in other users' repositories are undiscoverable.
 * Constellation indexes the full AT Protocol firehose so we can ask
 * "which records across the entire network link to URI X?"
 *
 * Mirrors Inkwell iOS ConstellationService: same endpoints, same pagination
 * strategy, same convenience method split (comment / recommend / mention).
 */
package uk.ewancroft.inkwell.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import uk.ewancroft.inkwell.data.model.bluesky.ConstellationBacklink
import uk.ewancroft.inkwell.data.model.bluesky.ConstellationResponse
import uk.ewancroft.inkwell.shared.constellation.ConstellationPagination
import uk.ewancroft.inkwell.shared.constellation.ConstellationBacklink as SharedBacklink
import uk.ewancroft.inkwell.shared.constellation.ConstellationResponse as SharedResponse

object ConstellationClient {
    private const val BASE_URL = "https://constellation.microcosm.blue"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // ── Backlink Query ───────────────────────────────────────────────────

    /** URL-encodes a query param value — subjects/sources/cursors can contain
     * `:`, `/`, `#`, `[`, `]` (AT-URIs, NSID paths) which must not be
     * interpolated raw into the query string. */
    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Finds all records that link to the given subject via the given source. */
    suspend fun getBacklinks(
        subject: String,
        source: String,
        limit: Int = 50,
        cursor: String? = null
    ): ConstellationResponse = withContext(Dispatchers.IO) {
        val urlBuilder = StringBuilder("$BASE_URL/xrpc/blue.microcosm.links.getBacklinks")
            .append("?subject=").append(enc(subject))
            .append("&source=").append(enc(source))
            .append("&limit=").append(limit)
        cursor?.let { urlBuilder.append("&cursor=").append(enc(it)) }

        val request = Request.Builder().url(urlBuilder.toString()).get().build()
        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
            if (!response.isSuccessful || bodyString == null) {
                throw java.io.IOException(
                    "Constellation getBacklinks failed: HTTP ${response.code}"
                )
            }
            json.decodeFromString(bodyString)
        }
    }

    // ── Pagination ───────────────────────────────────────────────────────

    /** Paginates through all backlink results. */
    suspend fun paginateBacklinks(
        subject: String,
        source: String,
        maxCount: Int = 200
    ): List<ConstellationBacklink> {
        val shared = ConstellationPagination.paginateBacklinks(
            fetchPage = { limit, cursor -> getBacklinks(subject, source, limit, cursor).toShared() },
            maxCount = maxCount
        )
        return shared.map { it.toAndroid() }
    }

    /**
     * Total recommend count for a document, across the whole network.
     *
     * Mirrors Inkwell iOS `fetchRecommendCount(for:)`: request a single
     * record first: if there's no next cursor the whole result set fit in
     * that page, so its size is the count. Otherwise fall back to full
     * pagination via [getRecommendBacklinks].
     */
    suspend fun getRecommendCount(documentUri: String): Int {
        val first = try {
            getBacklinks(
                subject = documentUri,
                source = "site.standard.graph.recommend:document",
                limit = 1
            )
        } catch (_: Exception) {
            return 0
        }
        if (first.cursor == null) return first.records.orEmpty().size
        return getRecommendBacklinks(documentUri).size
    }

    /** Recommends (standard.site graph edges) pointing at this document. */
    suspend fun getRecommendBacklinks(documentUri: String): List<ConstellationBacklink> =
        paginateBacklinks(documentUri, "site.standard.graph.recommend:document")

    /** Comments (Leaflet pub.leaflet.comment records) pointing at this document. */
    suspend fun getCommentBacklinks(documentUri: String): List<ConstellationBacklink> =
        paginateBacklinks(documentUri, "pub.leaflet.comment:subject")

    /**
     * Mentions in Bluesky posts: searches both link facets and embed.external URIs.
     * Deduplicates by (did, rkey) since a single post could have both a facet
     * link and an embed URI referencing the same document.
     */
    suspend fun getDocumentMentionBacklinks(url: String): List<ConstellationBacklink> = coroutineScope {
        val facets = async { paginateBacklinks(
            url, "app.bsky.feed.post:facets[].features[app.bsky.richtext.facet#link].uri"
        ) }
        val embeds = async { paginateBacklinks(
            url, "app.bsky.feed.post:embed.external.uri"
        ) }
        val f = facets.await()
        val e = embeds.await()
        ConstellationPagination.deduplicate((f + e).map { it.toShared() }).map { it.toAndroid() }
    }
}

// ── Type Conversions ─────────────────────────────────────────────────────

private fun ConstellationBacklink.toShared(): SharedBacklink = SharedBacklink(
    did = did,
    collection = collection,
    rkey = rkey
)

private fun ConstellationResponse.toShared(): SharedResponse = SharedResponse(
    records = records.orEmpty().map { it.toShared() },
    cursor = cursor
)

private fun SharedBacklink.toAndroid(): ConstellationBacklink = ConstellationBacklink(
    did = did,
    collection = collection,
    rkey = rkey
)
