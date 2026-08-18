package uk.ewancroft.inkwell.shared.text

/**
 * Shared number formatting utility for displaying counts
 * (likes, reposts, replies) in abbreviated form.
 *
 * Used by both platforms' Bluesky post embed rendering.
 */
object NumberFormat {
    /**
     * Abbreviates a count: 1500000 → "1M", 2300 → "2K", 42 → "42".
     */
    fun formatCount(count: Int): String = when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> count.toString()
    }
}
