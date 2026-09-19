package uk.ewancroft.inkwell.shared.offline

actual fun createOfflineSyncQueue(durableDirPath: String, legacyCacheDirPath: String): OfflineSyncQueue =
    OfflineSyncQueueIos(durableDirPath, legacyCacheDirPath)
