package uk.ewancroft.inkwell.shared.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TipPromptPolicyTest {

    @Test
    fun doesNotShowBeforeMinLaunches() {
        repeat(4) { count ->
            assertFalse(TipPromptPolicy.shouldShowTip(launchCount = count, nowEpochMillis = 0))
        }
    }

    @Test
    fun showsAfterMinLaunchesIfNeverShown() {
        assertTrue(TipPromptPolicy.shouldShowTip(launchCount = 5, lastShownEpochMillis = -1L, nowEpochMillis = 0))
        assertTrue(TipPromptPolicy.shouldShowTip(launchCount = 100, lastShownEpochMillis = -1L, nowEpochMillis = 0))
    }

    @Test
    fun suppressesWithinCooldown() {
        val now = 1_000_000_000_000L
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
        val sixDaysAgo = now - (6 * 24 * 60 * 60 * 1000)

        assertTrue(TipPromptPolicy.shouldShowTip(
            launchCount = 10,
            lastShownEpochMillis = sevenDaysAgo,
            nowEpochMillis = now
        ))
        assertFalse(TipPromptPolicy.shouldShowTip(
            launchCount = 10,
            lastShownEpochMillis = sixDaysAgo,
            nowEpochMillis = now
        ))
    }

    @Test
    fun showsExactlyAtCooldownBoundary() {
        val now = 1_000_000_000_000L
        val exactlySevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
        assertTrue(TipPromptPolicy.shouldShowTip(
            launchCount = 10,
            lastShownEpochMillis = exactlySevenDaysAgo,
            nowEpochMillis = now
        ))
    }
}
