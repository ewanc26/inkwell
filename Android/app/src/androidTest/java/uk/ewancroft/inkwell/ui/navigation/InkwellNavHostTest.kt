package uk.ewancroft.inkwell.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import uk.ewancroft.inkwell.testing.FakeOAuthSessionStore
import uk.ewancroft.inkwell.testing.InkwellInstrumentationEnvironment
import uk.ewancroft.inkwell.ui.theme.InkwellTheme
import javax.inject.Inject

/**
 * Auth-driven navigation regressions for [InkwellNavHost].
 *
 * `Android/AGENTS.md` warns that "authentication changes do not automatically
 * rebuild an existing Navigation Compose graph merely because
 * `startDestination` changes" — the login and logout transitions have to be
 * verified rather than assumed. That is exactly what these tests pin down, plus
 * the plain fact that each of the three roots composes at all.
 *
 * Auth state is a plain hoisted flag here rather than a faked session: the
 * assertion is about the navigation graph reacting to the flag, and there is no
 * way to synthesise a signed-in `AuthViewModel` without either a real PDS or a
 * fabricated one (see the "no mock data" rule in `AGENTS.md`). The signed-out
 * side of the same transition is covered end-to-end by
 * `MainActivityIntentDeliveryTest`.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class InkwellNavHostTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @Inject
    lateinit var sessionStore: FakeOAuthSessionStore

    @Before
    fun setUp() {
        InkwellInstrumentationEnvironment.prepare()
        hiltRule.inject()
        sessionStore.reset()
    }

    @After
    fun tearDown() {
        InkwellInstrumentationEnvironment.tearDown()
    }

    @Test
    fun unauthenticatedGraphStartsOnTheSignInSurfaceWithNoTabBar() {
        setNavHost(initiallyAuthenticated = false)

        composeRule.onNodeWithText(string(R.string.auth_continue)).assertIsDisplayed()
        // The tab bar belongs to the signed-in shell only.
        composeRule.onNodeWithText(Screen.Reader.label).assertDoesNotExist()
        composeRule.onNodeWithText(Screen.Discover.label).assertDoesNotExist()
        composeRule.onNodeWithText(Screen.Writer.label).assertDoesNotExist()
    }

    @Test
    fun authenticatingRebuildsTheGraphOntoTheReaderRoot() {
        val authenticated = setNavHost(initiallyAuthenticated = false)
        composeRule.onNodeWithText(string(R.string.auth_continue)).assertIsDisplayed()

        composeRule.runOnIdle { authenticated.value = true }
        composeRule.waitForIdle()

        // Changing startDestination alone is not enough for Navigation Compose;
        // if the graph is not rebuilt, the login destination stays on the stack.
        composeRule.onNodeWithText(string(R.string.auth_continue)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.reader_title)).assertIsDisplayed()
        composeRule.onNodeWithText(Screen.Reader.label).assertIsDisplayed()
        composeRule.onNodeWithText(Screen.Discover.label).assertIsDisplayed()
        composeRule.onNodeWithText(Screen.Writer.label).assertIsDisplayed()
    }

    @Test
    fun signingOutFromTheReaderReturnsToTheSignInSurface() {
        setNavHost(initiallyAuthenticated = true)
        composeRule.onNodeWithText(string(R.string.reader_title)).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(string(R.string.reader_sign_out))
            .assert(hasClickAction())
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.auth_continue)).assertIsDisplayed()
        composeRule.onNodeWithText(Screen.Reader.label).assertDoesNotExist()
    }

    @Test
    fun discoverRootLaunchesFromTheTabBar() {
        setNavHost(initiallyAuthenticated = true)

        composeRule.onNodeWithText(Screen.Discover.label).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.discover_search)).assertExists()
        composeRule.onNodeWithText(string(R.string.discover_search_hint)).assertExists()
    }

    @Test
    fun writerRootLaunchesFromTheTabBar() {
        setNavHost(initiallyAuthenticated = true)

        composeRule.onNodeWithText(Screen.Writer.label).performClick()
        composeRule.waitForIdle()

        // The editor's own fields, not the tab label, prove the Writer root composed.
        composeRule.onNodeWithText(string(R.string.writer_title)).assertExists()
        composeRule.onNodeWithText(string(R.string.writer_content_markdown)).assertExists()
    }

    @Test
    fun tabBarRoundTripDoesNotLoseTheReaderRoot() {
        setNavHost(initiallyAuthenticated = true)

        composeRule.onNodeWithText(Screen.Discover.label).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(Screen.Reader.label).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(string(R.string.reader_title)).assertIsDisplayed()
    }

    /**
     * Mounts the nav host with a hoisted auth flag and returns it, so a test can
     * flip login/logout the same way `MainActivity` does.
     */
    private fun setNavHost(initiallyAuthenticated: Boolean): MutableState<Boolean> {
        val authenticated = mutableStateOf(initiallyAuthenticated)
        composeRule.setContent {
            InkwellTheme {
                InkwellNavHost(
                    isAuthenticated = authenticated.value,
                    onSignOut = { authenticated.value = false },
                )
            }
        }
        composeRule.waitForIdle()
        return authenticated
    }

    private fun string(@StringRes id: Int): String =
        composeRule.activity.getString(id)
}
