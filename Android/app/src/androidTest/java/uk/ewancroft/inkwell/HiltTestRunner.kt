package uk.ewancroft.inkwell

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumentation runner that swaps [InkwellApp] for Hilt's generated
 * [HiltTestApplication].
 *
 * `@HiltAndroidTest` builds its object graph from the *application's* Hilt
 * component, so the app under test has to be one Hilt controls. Two
 * consequences worth knowing before reading any test in this source set:
 *
 * 1. `@TestInstallIn` replacements (see `testing/TestOAuthModule.kt` and
 *    `testing/TestNetworkModule.kt`) only take effect because of this swap.
 * 2. [InkwellApp.onCreate] does **not** run, so WorkManager is never
 *    initialised for us — and the manifest deliberately removes WorkManager's
 *    own `androidx.startup` initializer. Tests that can reach
 *    `WorkManager.getInstance()` initialise it themselves; see
 *    `testing/InkwellInstrumentationEnvironment.kt`.
 *
 * Wired in via `testInstrumentationRunner` in `app/build.gradle.kts`.
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
