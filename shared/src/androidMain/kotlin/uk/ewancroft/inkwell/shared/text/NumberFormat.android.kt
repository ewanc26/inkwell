package uk.ewancroft.inkwell.shared.text

import java.text.NumberFormat
import java.util.Locale

actual object NumberFormat {
    actual fun formatCount(count: Int): String {
        val locale = Locale.getDefault()
        val formatter = NumberFormat.getIntegerInstance(locale)
        return when {
            count >= 1_000_000 -> "${formatter.format(count / 1_000_000)}M"
            count >= 1_000 -> "${formatter.format(count / 1_000)}K"
            else -> formatter.format(count)
        }
    }
}
