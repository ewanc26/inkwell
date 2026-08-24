package uk.ewancroft.inkwell.util

import android.content.Context
import coil.imageLoader

/**
 * Thin wrapper around Coil's singleton ImageLoader (configured with a
 * bounded disk cache in InkwellApp.newImageLoader) for the Settings
 * "Clear Cache" action. No size-tracking infrastructure of its own --
 * just reads Coil's own disk cache size.
 */
object ImageCacheManager {
    fun currentSizeBytes(context: Context): Long =
        context.imageLoader.diskCache?.size ?: 0L

    fun clear(context: Context) {
        val loader = context.imageLoader
        loader.memoryCache?.clear()
        loader.diskCache?.clear()
    }
}
