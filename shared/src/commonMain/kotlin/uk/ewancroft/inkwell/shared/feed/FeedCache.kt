package uk.ewancroft.inkwell.shared.feed

/**
 * Platform-agnostic interface for a local feed-item cache.
 *
 * The cache stores [CachedFeedItem] objects serialized as JSON.  The
 * actual storage backend is platform-specific:
 * - **Android**: writes to the app's internal cache directory as a
 *   JSON file.
 * - **iOS**: writes to the app's caches directory as a JSON file.
 *
 * Both backends are constructed via expect/actual factories that
 * receive the platform directory path at creation time.
 */
interface FeedCache {

    /**
     * Replace the entire cache contents with [items].
     * Previous entries are discarded.
     */
    suspend fun save(items: List<CachedFeedItem>)

    /**
     * Append or update items in the cache.  Existing items with the
     * same [CachedFeedItem.uri] are replaced; new items are appended.
     */
    suspend fun upsert(items: List<CachedFeedItem>)

    /**
     * Remove a single item by its AT URI.
     */
    suspend fun remove(uri: String)

    /**
     * Load up to [limit] cached items, most recently cached first.
     */
    suspend fun load(limit: Int = 200): List<CachedFeedItem>

    /**
     * Discard all cached data.
     */
    suspend fun clear()
}
