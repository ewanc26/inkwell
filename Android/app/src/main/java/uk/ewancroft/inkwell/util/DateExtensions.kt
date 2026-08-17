package uk.ewancroft.inkwell.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .withZone(ZoneId.systemDefault())

fun String.formatPublishedDate(): String {
    return try {
        val instant = Instant.parse(this)
        dateFormatter.format(instant)
    } catch (_: Exception) {
        this.take(10)
    }
}
