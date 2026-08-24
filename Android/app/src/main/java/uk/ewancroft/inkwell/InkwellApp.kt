/**
 * Application entry point for the Inkwell Android client.
 *
 * An AT Protocol reader/writer for the standard.site publishing ecosystem.
 * Hilt-annotated for dependency injection — the Dagger graph is built here
 * so every @AndroidEntryPoint activity has its dependencies wired.
 *
 * Mirror of Inkwell iOS's App struct: no custom Application logic yet,
 * but this is where WorkManager initialisation and theme-level config would live.
 *
 * Also supplies Coil's default ImageLoader with a bounded disk cache
 * (see ImageLoaderFactory below) so "cache size" is a real, controllable
 * thing rather than Coil's unbounded-by-default disk cache.
 */
package uk.ewancroft.inkwell

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.hilt.work.HiltWorkerFactory
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import dagger.hilt.android.HiltAndroidApp
import uk.ewancroft.inkwell.data.remote.InkwellNotificationManager
import javax.inject.Inject

@HiltAndroidApp
class InkwellApp : Application(), ImageLoaderFactory {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationManager: InkwellNotificationManager

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
        notificationManager.schedulePeriodicPoll()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(IMAGE_CACHE_MAX_BYTES)
                    .build()
            }
            .build()
    }

    companion object {
        /** 150 MB is generous for article/leaflet thumbnails without letting
         *  the cache grow unbounded on constrained devices. */
        const val IMAGE_CACHE_MAX_BYTES = 150L * 1024 * 1024
    }
}
