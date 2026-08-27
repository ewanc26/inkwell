package uk.ewancroft.inkwell.shared.feed

/**
 * Android factory — creates a [FeedCache] backed by a JSON file.
 *
 * @param cacheDirPath Absolute path to `context.cacheDir`.
 */
actual fun createFeedCache(cacheDirPath: String): FeedCache = FeedCacheAndroid(cacheDirPath)
