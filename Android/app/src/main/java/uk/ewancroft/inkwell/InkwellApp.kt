/**
 * Application entry point for the Inkwell Android client.
 *
 * An AT Protocol reader/writer for the standard.site publishing ecosystem.
 * Hilt-annotated for dependency injection — the Dagger graph is built here
 * so every @AndroidEntryPoint activity has its dependencies wired.
 *
 * Mirror of Inkwell iOS's App struct: no custom Application logic yet,
 * but this is where WorkManager initialisation and theme-level config would live.
 */
package uk.ewancroft.inkwell

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class InkwellApp : Application()
