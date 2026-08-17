package uk.ewancroft.inkwell.shared.policy

/**
 * Shared notification polling policy constants and decision logic.
 *
 * The actual I/O (fetching subscriptions, documents, sending platform
 * notifications) stays native in each app. This object captures the
 * policy numbers and pure-decision logic that are identical on both
 * platforms.
 *
 * Mirrors iOS `NotificationManager.pollForNewDocuments` and Android
 * `InkwellNotificationManager.pollForNewDocuments` — same retention
 * numbers, same first-poll baseline, same sort order.
 */
object NotificationPolicy {

    /** Maximum number of document URIs to remember as "seen". */
    const val MAX_SEEN_URIS: Int = 500

    /** Maximum number of notifications to retain in the in-app list. */
    const val MAX_NOTIFICATIONS: Int = 50

    /** Minimum number of new documents before showing a summary notification
     *  instead of individual per-document notifications. */
    const val SUMMARY_THRESHOLD: Int = 2

    /**
     * Returns true if this is the first poll (no previous poll timestamp).
     *
     * @param lastPollEpochMillis Epoch millis of the last poll, or -1 if never polled.
     */
    fun isFirstPoll(lastPollEpochMillis: Long = -1L): Boolean = lastPollEpochMillis == -1L

    /**
     * Determines whether to show a single-document notification or a summary.
     *
     * @param newDocCount Number of new documents discovered this poll.
     */
    fun notificationStyle(newDocCount: Int): NotificationStyle = when {
        newDocCount >= SUMMARY_THRESHOLD -> NotificationStyle.Summary(newDocCount)
        newDocCount == 1 -> NotificationStyle.Single
        else -> NotificationStyle.None
    }

    /**
     * Trims a list of URIs to at most [MAX_SEEN_URIS], keeping the most recent.
     * The caller should pass URIs in chronological order (oldest first).
     */
    fun trimSeenUris(seenUris: List<String>): List<String> {
        return if (seenUris.size > MAX_SEEN_URIS) {
            seenUris.takeLast(MAX_SEEN_URIS)
        } else {
            seenUris
        }
    }

    /**
     * Trims a notification list to at most [MAX_NOTIFICATIONS], keeping the newest.
     * The caller should pass notifications newest-first.
     */
    fun trimNotifications(notifications: List<Any>): List<Any> {
        return if (notifications.size > MAX_NOTIFICATIONS) {
            notifications.take(MAX_NOTIFICATIONS)
        } else {
            notifications
        }
    }
}

sealed interface NotificationStyle {
    data object None : NotificationStyle
    data object Single : NotificationStyle
    data class Summary(val count: Int) : NotificationStyle
}
