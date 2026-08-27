package uk.ewancroft.inkwell.shared.feed

/**
 * iOS factory — creates a [FeedCache] backed by a JSON file.
 *
 * @param cacheDirPath Absolute path to the app's caches directory.
 */
actual fun createFeedCache(cacheDirPath: String): FeedCache = FeedCacheIos(cacheDirPath)
