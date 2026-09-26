package uk.ewancroft.inkwell.ui.accessibility

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.ewancroft.inkwell.HiltTestActivity
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.testing.InkwellInstrumentationEnvironment
import uk.ewancroft.inkwell.ui.auth.LoginScreen
import uk.ewancroft.inkwell.ui.reader.MarkdownRendererView
import uk.ewancroft.inkwell.ui.reader.ReaderScreen
import uk.ewancroft.inkwell.ui.theme.InkwellTheme

/**
 * TalkBack and large-font smoke coverage for the surfaces a signed-out or
 * newly-signed-in reader hits first.
 *
 * Two things are asserted, both of which only a real device answers honestly:
 *
 * 1. **Accessible names.** Every control a screen reader can reach must announce
 *    something. Icon-only actions are the usual regression — a
 *    `contentDescription` dropped during a refactor leaves a button TalkBack
 *    reads as "button".
 * 2. **Font scale.** The reader's own font-size preference and the system font
 *    scale both land on `LocalDensity`, so overriding it here reproduces the
 *    largest-text case. Controls must stay present, named, and clickable — not
 *    merely still laid out.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AccessibilitySemanticsTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Before
    fun setUp() {
        InkwellInstrumentationEnvironment.prepare()
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        InkwellInstrumentationEnvironment.tearDown()
    }

    @Test
    fun signInSurfaceControlsAreNamedAndActionable() {
        setContentAtFontScale(1f) { LoginScreen() }

        composeRule.onNodeWithText(string(R.string.auth_continue))
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
        composeRule.onNodeWithText(string(R.string.auth_about))
            .assertIsDisplayed()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
        // The handle field is labelled by visible text, not only by a hint.
        composeRule.onNodeWithText(string(R.string.auth_handle)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.auth_handle_placeholder)).assertIsDisplayed()
    }

    @Test
    fun signInSurfaceSurvivesTheLargestFontScale() {
        setContentAtFontScale(LARGEST_FONT_SCALE) { LoginScreen() }

        // assertExists rather than assertIsDisplayed: at 2x the sign-in column
        // can extend past the viewport, which is a layout question. What must not
        // regress is that the control is still composed, still named, and still
        // operable by a screen reader.
        composeRule.onNodeWithText(string(R.string.auth_continue))
            .assertExists()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
        composeRule.onNodeWithText(string(R.string.auth_about))
            .assertExists()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
        composeRule.onNodeWithText(string(R.string.auth_handle_placeholder)).assertExists()
    }

    @Test
    fun readerTopBarActionsAreAllNamedForTalkBack() {
        setContentAtFontScale(1f) { ReaderScreen() }

        val iconOnlyActions = listOf(
            R.string.reader_sign_out,
            R.string.reader_notifications,
            R.string.reader_about,
            R.string.reader_settings,
            R.string.reader_refresh,
        )
        for (action in iconOnlyActions) {
            composeRule.onNodeWithContentDescription(string(action))
                .assertExists()
                .assert(hasClickAction())
                .assert(hasAccessibleName)
        }
    }

    @Test
    fun readerTopBarActionsStayNamedAtTheLargestFontScale() {
        setContentAtFontScale(LARGEST_FONT_SCALE) { ReaderScreen() }

        composeRule.onNodeWithContentDescription(string(R.string.reader_sign_out))
            .assertExists()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
        composeRule.onNodeWithContentDescription(string(R.string.reader_refresh))
            .assertExists()
            .assert(hasClickAction())
            .assert(hasAccessibleName)
    }

    @Test
    fun renderedMarkdownHeadingsCarryTheHeadingTrait() {
        val markdown = """
            # $HEADING_ONE

            Body copy that must not be announced as a heading.

            ## $HEADING_TWO
        """.trimIndent()

        setContentAtFontScale(LARGEST_FONT_SCALE) { MarkdownRendererView(markdown = markdown) }

        // TalkBack's heading navigation is the only way to skim a long document,
        // so the trait is load-bearing, not decorative.
        composeRule.onNodeWithText(HEADING_ONE).assertExists().assert(isHeading())
        composeRule.onNodeWithText(HEADING_TWO).assertExists().assert(isHeading())
        composeRule.onNodeWithText(BODY_COPY, substring = true)
            .assertExists()
            .assert(isHeading().not())
    }

    /**
     * Mounts [content] with [LocalDensity] rewritten to [fontScale] — the same
     * mechanism the app's own font-size preference uses, so the override composes
     * with it rather than fighting it.
     */
    private fun setContentAtFontScale(fontScale: Float, content: @Composable () -> Unit) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
            ) {
                InkwellTheme { content() }
            }
        }
        composeRule.waitForIdle()
    }

    private val hasAccessibleName = SemanticsMatcher("has a non-empty accessible name") { node ->
        val described = node.config.getOrNull(SemanticsProperties.ContentDescription)
            ?.any { it.isNotBlank() } == true
        val labelled = node.config.getOrNull(SemanticsProperties.Text)
            ?.any { it.text.isNotBlank() } == true
        described || labelled
    }

    private fun string(@StringRes id: Int): String = composeRule.activity.getString(id)

    private companion object {
        const val LARGEST_FONT_SCALE = 2f
        const val HEADING_ONE = "Instrumentation heading"
        const val HEADING_TWO = "Second level heading"
        const val BODY_COPY = "Body copy that must not be announced"
    }
}
