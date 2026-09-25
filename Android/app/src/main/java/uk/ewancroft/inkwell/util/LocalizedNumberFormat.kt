package uk.ewancroft.inkwell.util

import java.text.NumberFormat

fun formatLocalizedInteger(value: Int, numberFormat: NumberFormat = NumberFormat.getIntegerInstance()): String =
    numberFormat.format(value)

fun formatLocalizedPercentage(
    fraction: Float,
    numberFormat: NumberFormat = NumberFormat.getPercentInstance(),
): String {
    numberFormat.maximumFractionDigits = 0
    return numberFormat.format(fraction.toDouble())
}
