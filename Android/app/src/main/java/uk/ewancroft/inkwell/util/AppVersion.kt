package uk.ewancroft.inkwell.util

import android.content.Context
import android.os.Build

fun appVersionString(context: Context): String = try {
    val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkg.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        pkg.versionCode.toLong()
    }
    "Version ${pkg.versionName} ($code)"
} catch (_: Exception) {
    "Version unknown"
}
