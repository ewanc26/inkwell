package uk.ewancroft.inkwell.shared.offline

actual fun createOfflineContentCache(cacheDirPath: String): OfflineContentCache =
    OfflineContentCacheAndroid(cacheDirPath)
