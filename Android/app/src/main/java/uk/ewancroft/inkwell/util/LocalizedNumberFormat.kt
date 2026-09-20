package uk.ewancroft.inkwell.util

import java.text.NumberFormat

fun formatLocalizedInteger(value: Int, numberFormat: NumberFormat = NumberFormat.getIntegerInstance()): String =
    numberFormat.format(value)
