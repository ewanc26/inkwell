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
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import uk.ewancroft.inkwell.data.remote.InkwellNotificationManager
import javax.inject.Inject

@HiltAndroidApp
class InkwellApp : Application() {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationManager: InkwellNotificationManager

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
        notificationManager.schedulePeriodicPoll()
    }
}
