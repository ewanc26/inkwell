package uk.ewancroft.inkwell

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Launch-intent configuration for testing and screenshot capture.
 * Mirrors iOS's `TestingMode` (`-testing`).
 *
 * Testing mode signs in with the real session and reads real records over
 * the network — feed, publications, documents, comments and verification
 * state are all genuine. What it intercepts is every write: the UI stays
 * fully interactive, but the moment an action would send something outward
 * it stops at the network boundary and says so.
 *
 * It deliberately does not substitute mock data for anything. The fixtures
 * this replaced hid real bugs on both platforms, because the only thing
 * anyone ever looked at was the fake.
 *
 * Enable with `--ez testing true`; deep-link a tab with `--es tab reader`.
 */
object TestingConfig {
    /** Intercepts every outbound mutation while leaving reads untouched. */
    var enabled: Boolean = false

    /** Tab to open on launch. */
    var tab: String = "reader"

    /**
     * Suppresses the notification permission prompt and the Ko-fi tip
     * prompt. Not writes, but they steal focus mid-capture.
     */
    val suppressesInterruptions: Boolean get() = enabled

    /**
     * The blocked operation, in words a person reads. Non-null shows the
     * "Testing mode" dialog at the nav-host level, so the explanation isn't
     * buried under a feature's own error banner.
     */
    val blockedAction = MutableStateFlow<String?>(null)

    fun report(action: String) {
        blockedAction.value = action
    }

    fun clear() {
        blockedAction.value = null
    }

    const val MESSAGE = "You're in testing mode, so this action will not hit the network."
}

/**
 * Thrown by the [uk.ewancroft.inkwell.data.repository.PdsRepository] write
 * choke points when testing mode is on. Carries the same wording as the
 * dialog so a screen that surfaces its own error still explains the reason.
 */
class TestingModeException(action: String) : Exception(
    "${TestingConfig.MESSAGE}\n\n$action was not sent.",
)
