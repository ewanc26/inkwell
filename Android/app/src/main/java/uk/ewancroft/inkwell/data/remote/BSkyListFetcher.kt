package uk.ewancroft.inkwell.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.model.bluesky.GetListResponse
import uk.ewancroft.inkwell.shared.xrpc.XrpcEndpoints
import java.util.concurrent.TimeUnit

/**
 * Fetches Bluesky list members from the public API (public.api.bsky.app).
 * No authentication needed. Used to show Inkwell's supporters list in the
 * credits screen — see [uk.ewancroft.inkwell.shared.support.SupportersList].
 */
object BSkyListFetcher {
    private const val TAG = "BSkyListFetcher"
    private const val PAGE_LIMIT = 100

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches every member of the list at [listUri], following pagination
     * cursors until exhausted. Returns an empty list on any failure rather
     * than throwing — this powers a "credits" section, not a critical path.
     */
    suspend fun fetchListMembers(listUri: String): List<BlueskyProfile> {
        return withContext(Dispatchers.IO) {
            val members = mutableListOf<BlueskyProfile>()
            var cursor: String? = null

            try {
                do {
                    val encodedUri = java.net.URLEncoder.encode(listUri, "UTF-8")
                    var url = "${XrpcEndpoints.PUBLIC_BSKY_API}${XrpcEndpoints.GRAPH_GET_LIST}?list=$encodedUri&limit=$PAGE_LIMIT"
                    cursor?.let { url += "&cursor=${java.net.URLEncoder.encode(it, "UTF-8")}" }

                    val request = Request.Builder().url(url).build()
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        Log.w(TAG, "Failed to fetch list $listUri: HTTP ${response.code}")
                        return@withContext members
                    }

                    val body = response.body?.string() ?: return@withContext members
                    val page = json.decodeFromString<GetListResponse>(body)
                    members += page.items.map { it.subject }
                    cursor = page.cursor
                } while (cursor != null)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Bluesky list: ${e.message}")
            }

            members
        }
    }
}
