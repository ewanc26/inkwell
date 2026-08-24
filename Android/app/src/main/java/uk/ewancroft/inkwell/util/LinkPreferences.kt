package uk.ewancroft.inkwell.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Governs how content links (article body links, post links, discover
 * results) open -- in an in-app Custom Tab, or handed off to the system
 * browser via a plain ACTION_VIEW intent. Deliberately separate from
 * login's OAuth CustomTabsIntent, which always uses Custom Tabs regardless
 * of this preference -- that's an auth-flow requirement, not a content
 * link. Mirrors iOS LinkPreferences.swift.
 */
object LinkPreferences {
    private const val PREFS_NAME = "inkwell_links"
    private const val OPEN_LINKS_IN_APP_KEY = "open_links_in_app"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getOpenLinksInApp(context: Context): Boolean = prefs(context).getBoolean(OPEN_LINKS_IN_APP_KEY, true)

    fun setOpenLinksInApp(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(OPEN_LINKS_IN_APP_KEY, enabled).apply()
    }

    /** Opens a content link per the current preference, falling back to the system browser on failure. */
    fun openContentUrl(context: Context, url: String) {
        try {
            if (getOpenLinksInApp(context)) {
                CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(url))
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } catch (_: Exception) {}
    }
}
