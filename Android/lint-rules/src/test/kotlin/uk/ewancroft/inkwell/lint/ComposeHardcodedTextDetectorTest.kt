package uk.ewancroft.inkwell.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

/**
 * Hermetic unit tests for [ComposeHardcodedTextDetector], run via
 * `./gradlew :lint-rules:test` — not part of `:app:testDebugUnitTest`.
 */
class ComposeHardcodedTextDetectorTest {

    // `TestLintTask.files()` replaces the file list on every call rather than
    // appending to it, so every test must pass all of its files (stub
    // included) in a single call — never chain a second `.files(...)`.
    private fun lintTask(vararg testFiles: TestFile) = lint()
        .allowMissingSdk()
        .issues(ComposeHardcodedTextDetector.ISSUE)
        .files(*testFiles)

    @Test
    fun `flags a hardcoded string literal in a Compose Text call`() {
        lintTask(
            COMPOSE_TEXT_STUB,
            kotlin(
                """
                package test

                import androidx.compose.material3.Text

                fun greet() {
                    Text("Hello there")
                }
                """,
            ).indented(),
        )
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `allows a non-literal argument such as a stringResource call result`() {
        lintTask(
            COMPOSE_TEXT_STUB,
            kotlin(
                """
                package test

                import androidx.compose.material3.Text

                fun greet(label: String) {
                    Text(label)
                }
                """,
            ).indented(),
        )
            .run()
            .expectClean()
    }

    @Test
    fun `ignores decorative glyphs with no letters`() {
        lintTask(
            COMPOSE_TEXT_STUB,
            kotlin(
                """
                package test

                import androidx.compose.material3.Text

                fun separator() {
                    Text("·")
                }
                """,
            ).indented(),
        )
            .run()
            .expectClean()
    }

    @Test
    fun `does not flag an unrelated function named Text when it resolves cleanly`() {
        // Full type information is available in this synthetic project, so the
        // detector resolves the call to the local (non-Compose) `Text` and
        // filters it out — it only falls back to name-only matching when
        // resolution fails (e.g. incomplete classpaths in a real module).
        lintTask(
            kotlin(
                """
                package test

                fun Text(value: String) {}

                fun call() {
                    Text("some literal")
                }
                """,
            ).indented(),
        )
            .run()
            .expectClean()
    }

    companion object {
        private val COMPOSE_TEXT_STUB = kotlin(
            """
            package androidx.compose.material3

            fun Text(text: String) {}
            """,
        ).indented()
    }
}
