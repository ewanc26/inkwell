package uk.ewancroft.inkwell.util

import java.text.NumberFormat

private const val BYTES_PER_MEGABYTE = 1024.0 * 1024.0

fun formatCacheSize(bytes: Long, numberFormat: NumberFormat = NumberFormat.getNumberInstance()): String {
    val megabytes = bytes / BYTES_PER_MEGABYTE
    if (megabytes < 0.1) return "Empty"

    numberFormat.minimumFractionDigits = 1
    numberFormat.maximumFractionDigits = 1
    return "${numberFormat.format(megabytes)} MB"
}
