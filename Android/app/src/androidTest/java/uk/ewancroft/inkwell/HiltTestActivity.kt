package uk.ewancroft.inkwell

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * A bare Hilt-enabled host for composable-level instrumentation tests.
 *
 * Screens under test call `hiltViewModel()`, which needs an `@AndroidEntryPoint`
 * Activity to resolve against — but [MainActivity] brings its own content,
 * splash, and intent handling with it. This Activity provides the Hilt
 * ViewModel factory and nothing else, so a test can mount exactly the
 * composable it means to exercise.
 *
 * Declared in `app/src/androidTest/AndroidManifest.xml`.
 */
@AndroidEntryPoint
class HiltTestActivity : ComponentActivity()
