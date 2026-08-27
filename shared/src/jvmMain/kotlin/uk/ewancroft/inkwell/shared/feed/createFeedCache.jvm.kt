package uk.ewancroft.inkwell.shared.feed

actual fun createFeedCache(cacheDirPath: String): FeedCache = FeedCacheJvm(cacheDirPath)
