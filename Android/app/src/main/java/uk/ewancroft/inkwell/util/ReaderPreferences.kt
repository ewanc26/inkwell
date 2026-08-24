package uk.ewancroft.inkwell.util

import android.content.Context

/**
 * Persisted preference for the order reader feed items are shown in.
 * Mirrors iOS ReaderSortSettings.swift.
 */
object ReaderPreferences {
    private const val PREFS_NAME = "inkwell_reader"
    private const val SORT_ORDER_KEY = "sort_order"

    enum class SortOrder { NEWEST_FIRST, OLDEST_FIRST }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSortOrder(context: Context): SortOrder =
        prefs(context).getString(SORT_ORDER_KEY, null)?.let {
            try { SortOrder.valueOf(it) } catch (e: IllegalArgumentException) { null }
        } ?: SortOrder.NEWEST_FIRST

    fun setSortOrder(context: Context, order: SortOrder) {
        prefs(context).edit().putString(SORT_ORDER_KEY, order.name).apply()
    }
}
