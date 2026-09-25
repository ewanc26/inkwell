package uk.ewancroft.inkwell.util

import java.text.NumberFormat

/** Formats a 0..1 fraction as a locale-aware percentage (symbol placement, digit shapes, etc.). */
fun Float.formatAsPercentage(): String =
    NumberFormat.getPercentInstance().apply { maximumFractionDigits = 0 }.format(this.toDouble())
