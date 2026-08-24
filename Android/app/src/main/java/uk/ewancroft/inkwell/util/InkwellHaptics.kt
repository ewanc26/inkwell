package uk.ewancroft.inkwell.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Every haptic event in the app. Mirrors iOS's InkwellHaptics enum in
 * InkwellTheme.swift so the "feel" is consistent across platforms.
 * Compose has no direct equivalent of UIKit's impact/notification
 * feedback generators, so this maps onto the closest HapticFeedbackType
 * constants instead.
 */
class InkwellHaptics(
    private val hapticFeedback: HapticFeedback,
    private val enabled: () -> Boolean,
) {
    /** Light tap -- button presses, cell selection, toggle switches. */
    fun light() {
        if (enabled()) hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    /** Medium press -- publish button, confirmations, destructive warnings. */
    fun medium() {
        if (enabled()) hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** Success -- subscription confirmed, post published, comment posted. */
    fun success() {
        if (enabled()) hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
    }

    /** Warning/error -- verification failed, publish failed, network error. */
    fun error() {
        if (enabled()) hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
    }

    /** Selection change -- tab switches, picker value changes. */
    fun selection() {
        if (enabled()) hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }
}

/** Remembers an [InkwellHaptics] instance gated on the "Haptics" accessibility setting. */
@Composable
fun rememberInkwellHaptics(): InkwellHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    return InkwellHaptics(hapticFeedback) { AccessibilityPreferences.getHapticsEnabled(context) }
}
