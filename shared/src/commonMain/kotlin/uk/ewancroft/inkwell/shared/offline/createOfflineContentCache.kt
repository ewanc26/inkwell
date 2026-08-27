package uk.ewancroft.inkwell.shared.offline

/** Creates the platform file-backed cache for complete offline records. */
expect fun createOfflineContentCache(cacheDirPath: String): OfflineContentCache
