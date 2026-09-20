package uk.ewancroft.inkwell.shared.jetstream

/** Bounded reconnect delays shared by platform Jetstream consumers. */
object JetstreamRetryPolicy {
    const val MAX_DELAY_MS = 60_000L

    fun delayMillis(attempt: Int): Long =
        (1_000L shl attempt.coerceIn(0, 6)).coerceAtMost(MAX_DELAY_MS)
}
