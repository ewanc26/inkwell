package uk.ewancroft.inkwell.shared.offline

actual fun createOfflineSyncQueue(cacheDirPath: String): OfflineSyncQueue =
    OfflineSyncQueueAndroid(cacheDirPath)
