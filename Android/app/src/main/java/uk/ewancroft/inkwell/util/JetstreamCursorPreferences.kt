package uk.ewancroft.inkwell.util

import android.content.Context

object JetstreamCursorPreferences {
    private const val PREFS_NAME = "jetstream_cursors"
    private const val MISSING = -1L

    internal fun cursorKey(dids: List<String>): String = dids.sorted().joinToString(",")

    fun get(context: Context, dids: List<String>): Long? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(cursorKey(dids), MISSING)
            .takeUnless { it == MISSING }

    fun put(context: Context, dids: List<String>, cursor: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(cursorKey(dids), cursor)
            .apply()
    }
}
