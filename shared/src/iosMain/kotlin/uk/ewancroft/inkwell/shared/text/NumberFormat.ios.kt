package uk.ewancroft.inkwell.shared.text

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual object NumberFormat {
    actual fun formatCount(count: Int): String {
        // NSNumberFormatter defaults `locale` to the current locale already.
        val formatter = NSNumberFormatter().apply {
            numberStyle = NSNumberFormatterDecimalStyle
        }
        fun localized(value: Int): String = formatter.stringFromNumber(NSNumber(int = value)) ?: value.toString()
        return when {
            count >= 1_000_000 -> "${localized(count / 1_000_000)}M"
            count >= 1_000 -> "${localized(count / 1_000)}K"
            else -> localized(count)
        }
    }
}
