package uk.ewancroft.inkwell.shared.feed

/**
 * Creates a platform-specific [FeedCache] instance.
 *
 * @param cacheDirPath The absolute path to the directory where the
 *   cache file should be stored.
 *   - **Android**: pass `context.cacheDir.absolutePath`.
 *   - **iOS**: pass the app's caches directory path.
 */
expect fun createFeedCache(cacheDirPath: String): FeedCache
