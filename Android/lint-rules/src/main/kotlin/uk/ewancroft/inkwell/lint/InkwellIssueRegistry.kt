package uk.ewancroft.inkwell.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/**
 * Entry point Android Lint discovers via `META-INF/services` (see
 * `src/main/resources/META-INF/services/com.android.tools.lint.client.api.IssueRegistry`)
 * when this module is added to the app's `lintChecks` configuration.
 *
 * Register every custom detector's [Issue] here — lint will not pick up a
 * detector just because it exists in this module; it must be listed below.
 */
class InkwellIssueRegistry : IssueRegistry() {

    override val issues: List<Issue> = listOf(
        ComposeHardcodedTextDetector.ISSUE,
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = 8

    override val vendor: Vendor = Vendor(
        vendorName = "Inkwell",
        identifier = "uk.ewancroft.inkwell.lint-rules",
        feedbackUrl = "https://github.com/ewanc26/inkwell/issues",
    )
}
