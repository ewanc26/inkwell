package uk.ewancroft.inkwell.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import uk.ewancroft.inkwell.data.model.bluesky.BSkyPostView
import uk.ewancroft.inkwell.data.model.bluesky.GetPostsResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Fetches Bluesky posts from the public API (public.api.bsky.app).
 * No authentication needed. Responses are cached in-memory.
 */
object BSkyPostFetcher {
    private const val TAG = "BSkyPostFetcher"
    private const val BASE_URL = "https://public.api.bsky.app"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val cache = ConcurrentHashMap<String, BSkyPostView>()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Fetches a single Bluesky post by its AT-URI.
     * Returns cached version if available.
     */
    suspend fun fetchPost(uri: String): BSkyPostView? {
        cache[uri]?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/xrpc/app.bsky.feed.getPosts?uris=${java.net.URLEncoder.encode(uri, "UTF-8")}"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to fetch post: HTTP ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: return@withContext null
                val result = json.decodeFromString<GetPostsResponse>(body)
                val post = result.posts.firstOrNull()

                post?.let { cache[uri] = it }
                post
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching Bluesky post: ${e.message}")
                null
            }
        }
    }
}
