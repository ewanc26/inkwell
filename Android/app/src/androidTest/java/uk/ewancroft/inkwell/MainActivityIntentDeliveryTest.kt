package uk.ewancroft.inkwell

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.annotation.StringRes
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uk.ewancroft.inkwell.deeplink.HttpsDeepLinkPolicy
import uk.ewancroft.inkwell.testing.FakeOAuthSessionStore
import uk.ewancroft.inkwell.testing.InkwellInstrumentationEnvironment
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Real-Activity delivery tests for the three distinct link types `MainActivity`
 * accepts, plus the lifecycle cases behind them.
 *
 * The policy objects themselves (`OAuthCallbackPolicy`, `ContentDeepLinkPolicy`,
 * `HttpsDeepLinkPolicy`) are already covered hermetically by Robolectric unit
 * tests. What only a device can answer is whether `launchMode="singleTask"`
 * actually routes a callback into the app once — on cold start, again through
 * `onNewIntent()` while warm, and *not* again when the same callback is
 * redelivered or when the process is recreated underneath it.
 *
 * Every assertion is bounded by [TIMEOUT_MS] and fails with a named reason, so
 * a regression reports itself rather than hanging an emulator.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityIntentDeliveryTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // The Activity is launched per-test with a specific intent, which is the
    // whole point here — so the rule must not launch one of its own.
    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var sessionStore: FakeOAuthSessionStore

    private val context: Context get() = ApplicationProvider.getApplicationContext()

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

    // ── Cold launch ─────────────────────────────────────────────────────

    @Test
    fun unauthenticatedColdLaunchReachesTheSignInSurface() {
        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()
            // Nothing was mistaken for an OAuth callback on a plain launch.
            awaitActivity(scenario, "no OAuth callback to have been handled") {
                it.handledOAuthCallback == null
            }
            assertNull(readActivity(scenario) { it.pendingDocumentUri.value })
        }
    }

    // ── OAuth callback: cold start, warm start, duplicate ───────────────

    @Test
    fun coldStartOAuthCallbackIsRoutedExactlyOnce() {
        val callback = "uk.ewancroft.inkwell:/callback?code=cold-code&state=cold-state"
        val fingerprint = fingerprintOf(callback)

        ActivityScenario.launch<MainActivity>(viewIntent(callback)).use { scenario ->
            awaitActivity(scenario, "the cold-start callback to be handed to the OAuth library") {
                it.handledOAuthCallback == fingerprint
            }
            // The exchange fails against the scripted offline transport, so the
            // app must stay on the sign-in surface rather than inventing a session.
            awaitSignInSurface()
        }
    }

    @Test
    fun warmSingleTaskCallbackIsDeliveredThroughOnNewIntent() {
        val callback = "uk.ewancroft.inkwell:/callback?code=warm-code&state=warm-state"
        val fingerprint = fingerprintOf(callback)

        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()
            assertNull(readActivity(scenario) { it.handledOAuthCallback })

            // singleTask means the running Activity receives the callback here,
            // not through a fresh onCreate().
            scenario.onActivity { it.onNewIntent(viewIntent(callback)) }

            awaitActivity(scenario, "the warm callback to be handed to the OAuth library") {
                it.handledOAuthCallback == fingerprint
            }
            // Handled deliveries are consumed, so a later recomposition cannot replay them.
            awaitActivity(scenario, "the handled warm delivery to be consumed") {
                it.pendingIntent.value == null
            }
        }
    }

    @Test
    fun duplicateCallbackDeliveryIsANoOp() {
        val callback = "uk.ewancroft.inkwell:/callback?code=dup-code&state=dup-state"
        val fingerprint = fingerprintOf(callback)

        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()

            scenario.onActivity { it.onNewIntent(viewIntent(callback)) }
            awaitActivity(scenario, "the first delivery to be handled and consumed") {
                it.handledOAuthCallback == fingerprint && it.pendingIntent.value == null
            }

            // Same callback URI, fresh Intent instance: exactly what a
            // redelivery looks like. It must not be exchanged a second time.
            scenario.onActivity { it.onNewIntent(viewIntent(callback)) }
            composeRule.waitForIdle()

            assertEquals(
                "a duplicate callback must not change the handled-callback guard",
                fingerprint,
                readActivity(scenario) { it.handledOAuthCallback },
            )
            // An unconsumed pending intent is the observable signature of the
            // no-op path: only a delivery that is actually handed to the OAuth
            // library clears it.
            assertNotNull(
                "the duplicate delivery should have been recognised and left unconsumed",
                readActivity(scenario) { it.pendingIntent.value },
            )
        }
    }

    @Test
    fun unrelatedCustomSchemeAndMalformedCallbacksAreIgnored() {
        val rejected = listOf(
            // Right scheme, wrong path — the manifest filter is exact, and so is
            // the runtime check.
            "uk.ewancroft.inkwell:/callback-extra?code=abc&state=xyz",
            "uk.ewancroft.inkwell:/other?code=abc&state=xyz",
            // Right shape, wrong app.
            "other.app:/callback?code=abc&state=xyz",
        )

        for (uri in rejected) {
            ActivityScenario.launch<MainActivity>(viewIntent(uri)).use { scenario ->
                awaitSignInSurface()
                composeRule.waitForIdle()
                assertNull(
                    "$uri must never be treated as an OAuth callback",
                    readActivity(scenario) { it.handledOAuthCallback },
                )
            }
        }
    }

    // ── Process recreation ──────────────────────────────────────────────

    @Test
    fun recreationKeepsTheSignInSurfaceAndDoesNotReplayTheCallback() {
        val callback = "uk.ewancroft.inkwell:/callback?code=recreate-code&state=recreate-state"
        val fingerprint = fingerprintOf(callback)

        ActivityScenario.launch<MainActivity>(viewIntent(callback)).use { scenario ->
            awaitActivity(scenario, "the callback to be handled before recreation") {
                it.handledOAuthCallback == fingerprint
            }

            scenario.recreate()

            // getIntent() still carries the callback after recreation, so the
            // guard has to survive with it — otherwise the fresh instance
            // re-exchanges a spent authorization code.
            awaitActivity(scenario, "the handled-callback guard to survive recreation") {
                it.handledOAuthCallback == fingerprint
            }
            awaitSignInSurface()
        }
    }

    @Test
    fun recreationOfAPlainLaunchKeepsTheSignInSurface() {
        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()
            scenario.recreate()
            awaitSignInSurface()
            assertNull(readActivity(scenario) { it.handledOAuthCallback })
        }
    }

    // ── inkwell://document (trusted custom scheme) ──────────────────────

    @Test
    fun warmContentDeepLinkHandsTheDocumentToTheReaderSynchronously() {
        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()

            scenario.onActivity { activity ->
                activity.onNewIntent(viewIntent(contentDeepLink(DOCUMENT_URI)))
                // Inkwell itself is the source of an inkwell://document AT-URI,
                // so it is trusted immediately — no verification round trip.
                // Asserting inside the same main-thread block makes this
                // race-free: the nav host cannot have consumed it yet.
                assertEquals(DOCUMENT_URI, activity.pendingDocumentUri.value)
            }
        }
    }

    @Test
    fun coldContentDeepLinkNavigatesAwayFromTheSignInSurface() {
        ActivityScenario.launch<MainActivity>(viewIntent(contentDeepLink(DOCUMENT_URI))).use { scenario ->
            assertEquals(
                DOCUMENT_URI,
                readActivity(scenario) { ContentDeepLinkPolicy.documentUri(it.intent) },
            )
            awaitNoSignInSurface()
        }
    }

    @Test
    fun contentDeepLinksForNonDocumentRecordsAreIgnored() {
        val publicationLink = contentDeepLink("at://did:plc:example/site.standard.publication/abc")

        ActivityScenario.launch<MainActivity>(viewIntent(publicationLink)).use { scenario ->
            awaitSignInSurface()
            assertNull(readActivity(scenario) { it.pendingDocumentUri.value })
        }
    }

    // ── https://inkwell.ewancroft.uk/open (unverified claim) ────────────

    @Test
    fun httpsUniversalLinkIsNeverHandedToTheReaderUnverified() {
        val link = "https://inkwell.ewancroft.uk/open?uri=${Uri.encode(DOCUMENT_URI)}"
        // The policy does recognise it as a candidate...
        assertEquals(DOCUMENT_URI, HttpsDeepLinkPolicy.candidateDocumentUri(viewIntent(link)))

        ActivityScenario.launch<MainActivity>(launcherIntent()).use { scenario ->
            awaitSignInSurface()

            scenario.onActivity { activity ->
                activity.onNewIntent(viewIntent(link))
                // ...but unlike the custom scheme, nothing is handed over
                // synchronously: an https link is an unverified claim anyone can
                // author, so it has to clear HttpsDeepLinkResolver first.
                assertNull(activity.pendingDocumentUri.value)
            }

            // Verification runs against the scripted offline transport, so it can
            // only fail — and a failed hand-off goes to the browser, never into
            // the Reader. There is no positive signal to wait on (the browser
            // hand-off clears itself as soon as it fires), so the invariant is
            // held open across a bounded settle window instead.
            assertStaysNull(
                scenario,
                "an unverified https claim must never reach the Reader",
            ) { it.pendingDocumentUri.value }
            awaitSignInSurface()
        }
    }

    @Test
    fun httpsLinksOutsideTheHandOffPathAreIgnored() {
        val link = "https://inkwell.ewancroft.uk/privacy?uri=${Uri.encode(DOCUMENT_URI)}"
        assertNull(HttpsDeepLinkPolicy.candidateDocumentUri(viewIntent(link)))

        ActivityScenario.launch<MainActivity>(viewIntent(link)).use { scenario ->
            awaitSignInSurface()
            assertNull(readActivity(scenario) { it.pendingDocumentUri.value })
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * `MainActivity` is `launchMode="singleTask"`, which is the behaviour under
     * test — and it is safe to drive with `ActivityScenario` because the
     * instrumentation invoker starts every launch with
     * `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`, so each `launch`
     * genuinely gets a fresh instance rather than silently reusing a task from
     * an earlier test. `EXTRA_TESTING` mirrors `--ez testing true`: it
     * suppresses the splash animation and the permission/tip prompts.
     */
    private fun launcherIntent(): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_TESTING, true)

    private fun viewIntent(uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .setClass(context, MainActivity::class.java)
            .putExtra(EXTRA_TESTING, true)

    private fun contentDeepLink(documentUri: String): String =
        "inkwell://document?uri=${Uri.encode(documentUri)}"

    private fun fingerprintOf(callback: String): String =
        checkNotNull(OAuthCallbackPolicy.deliveryFingerprint(Uri.parse(callback))) {
            "$callback should be recognised as an OAuth callback"
        }

    private fun string(@StringRes id: Int): String = context.getString(id)

    private fun signInSurfaceNodeCount(): Int =
        composeRule.onAllNodesWithText(string(R.string.auth_continue)).fetchSemanticsNodes().size

    private fun awaitSignInSurface() {
        composeRule.waitUntil(TIMEOUT_MS) { signInSurfaceNodeCount() > 0 }
    }

    private fun awaitNoSignInSurface() {
        composeRule.waitUntil(TIMEOUT_MS) { signInSurfaceNodeCount() == 0 }
    }

    private fun <T> readActivity(
        scenario: ActivityScenario<MainActivity>,
        read: (MainActivity) -> T,
    ): T {
        val captured = AtomicReference<Any?>(NOT_READ)
        scenario.onActivity { captured.set(read(it)) }
        val value = captured.get()
        if (value === NOT_READ) {
            throw AssertionError("ActivityScenario never handed back an Activity to read")
        }
        @Suppress("UNCHECKED_CAST")
        return value as T
    }

    /**
     * Holds a "this never happens" invariant open across a bounded window,
     * for the asynchronous paths that have no positive signal to wait on.
     */
    private fun assertStaysNull(
        scenario: ActivityScenario<MainActivity>,
        description: String,
        read: (MainActivity) -> Any?,
    ) {
        val deadline = SystemClock.uptimeMillis() + SETTLE_MS
        while (SystemClock.uptimeMillis() < deadline) {
            composeRule.waitForIdle()
            assertNull(description, readActivity(scenario, read))
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
    }

    /**
     * Polls [predicate] against the live Activity until it holds, bounded by
     * [TIMEOUT_MS]. Reading through `onActivity` rather than from inside
     * `waitUntil` keeps the main thread free while polling.
     */
    private fun awaitActivity(
        scenario: ActivityScenario<MainActivity>,
        description: String,
        predicate: (MainActivity) -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (SystemClock.uptimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (readActivity(scenario, predicate)) return
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        fail("Timed out after ${TIMEOUT_MS}ms waiting for $description")
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L
        const val SETTLE_MS = 2_000L
        const val POLL_INTERVAL_MS = 50L
        const val EXTRA_TESTING = "testing"
        const val DOCUMENT_URI = "at://did:plc:example/site.standard.document/instrumentation"
        val NOT_READ = Any()
    }
}
