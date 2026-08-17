package uk.ewancroft.inkwell.util

import android.content.Context
import android.content.SharedPreferences
import uk.ewancroft.inkwell.shared.policy.TipPromptPolicy

object TipPromptManager {

    private const val PREFS_NAME = "inkwell_tip_prompt"
    private const val KEY_LAUNCH_COUNT = "launch_count"
    private const val KEY_LAST_SHOWN = "last_shown_date"

    fun recordLaunch(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        prefs.edit().putInt(KEY_LAUNCH_COUNT, current + 1).apply()
    }

    fun shouldShowTip(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val lastShown = prefs.getLong(KEY_LAST_SHOWN, -1L)

        return TipPromptPolicy.shouldShowTip(
            launchCount = count,
            lastShownEpochMillis = lastShown,
            nowEpochMillis = System.currentTimeMillis(),
        )
    }

    fun markShown(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SHOWN, System.currentTimeMillis()).apply()
    }
}
