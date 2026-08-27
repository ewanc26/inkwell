package uk.ewancroft.inkwell.shared.offline

/** Creates the platform file-backed queue for mutations awaiting reconnect. */
expect fun createOfflineSyncQueue(cacheDirPath: String): OfflineSyncQueue
