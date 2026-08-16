package uk.ewancroft.inkwell

/**
 * Screenshot-mode configuration for UI capture automation.
 *
 * Mirrors the iOS `-screenshot` launch argument. MainActivity populates it
 * from the launch intent's extras so the Reader, Discover, and Writer
 * screens can render stable mock data without a live PDS session.
 */
object ScreenshotConfig {
    var enabled: Boolean = false
    var tab: String = "reader"
}
