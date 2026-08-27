package uk.ewancroft.inkwell.util

import android.content.Context
import uk.ewancroft.inkwell.shared.feed.createFeedCache
import uk.ewancroft.inkwell.shared.offline.createOfflineContentCache

/** Clears the shared reader caches while the image cache remains native to Coil. */
object OfflineContentCacheManager {
    suspend fun clear(context: Context) {
        val cachePath = context.cacheDir.absolutePath
        createFeedCache(cachePath).clear()
        createOfflineContentCache(cachePath).clear()
    }
}
