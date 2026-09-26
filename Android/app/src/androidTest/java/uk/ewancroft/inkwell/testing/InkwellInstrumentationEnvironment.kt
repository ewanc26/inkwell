package uk.ewancroft.inkwell.testing

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import uk.ewancroft.inkwell.TestingConfig

/**
 * The two pieces of process-wide setup every instrumentation test needs, in one
 * place so no test silently forgets one.
 */
object InkwellInstrumentationEnvironment {

    private var workManagerReady = false

    /**
     * Prepares the process for a test.
     *
     * - **WorkManager**: `HiltTestApplication` replaces [uk.ewancroft.inkwell.InkwellApp],
     *   so `WorkManager.initialize()` never runs, and the app manifest removes
     *   WorkManager's `androidx.startup` initializer on purpose. Anything that
     *   reached `WorkManager.getInstance()` would throw. The test initialiser
     *   gives it a synchronous, driver-backed instance that enqueues nothing
     *   real.
     * - **Testing mode**: suppresses the POST_NOTIFICATIONS permission prompt
     *   and the Ko-fi tip prompt, both of which are system/dialog surfaces that
     *   would steal focus and hang Compose assertions. It also skips
     *   `MainActivity`'s splash animation. Its write interception is irrelevant
     *   here — this suite performs no writes.
     */
    fun prepare() {
        TestingConfig.enabled = true
        TestingConfig.tab = "reader"
        TestingConfig.clear()
        if (!workManagerReady) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            WorkManagerTestInitHelper.initializeTestWorkManager(
                context,
                Configuration.Builder().setMinimumLoggingLevel(Log.ERROR).build(),
            )
            workManagerReady = true
        }
    }

    /** Undoes [prepare]'s global mutation so an ordinary launch is unaffected. */
    fun tearDown() {
        TestingConfig.clear()
        TestingConfig.enabled = false
        TestingConfig.tab = "reader"
    }
}
