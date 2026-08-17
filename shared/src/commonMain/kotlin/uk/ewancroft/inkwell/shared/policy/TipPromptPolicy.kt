package uk.ewancroft.inkwell.shared.policy

/**
 * Pure tip-prompt gating policy — identical on both platforms.
 *
 * The prompt appears after [MIN_LAUNCHES] app launches and is suppressed for
 * [COOLDOWN_DAYS] after dismissal.
 *
 * Mirrors iOS `TipPromptManager` and Android `TipPromptManager` — same
 * thresholds, same cooldown logic.
 */
object TipPromptPolicy {

    const val MIN_LAUNCHES: Int = 5
    const val COOLDOWN_DAYS: Long = 7L

    /**
     * Returns true if the tip prompt should be shown.
     *
     * @param launchCount Total number of recorded app launches.
     * @param lastShownEpochMillis Epoch millis of the last time the prompt was shown,
     *     or -1 if it has never been shown.
     * @param nowEpochMillis Current epoch millis (for testability).
     */
    fun shouldShowTip(
        launchCount: Int,
        lastShownEpochMillis: Long = -1L,
        nowEpochMillis: Long,
    ): Boolean {
        if (launchCount < MIN_LAUNCHES) return false

        if (lastShownEpochMillis == -1L) return true

        val diffDays = (nowEpochMillis - lastShownEpochMillis) / (1000 * 60 * 60 * 24)
        return diffDays >= COOLDOWN_DAYS
    }
}
