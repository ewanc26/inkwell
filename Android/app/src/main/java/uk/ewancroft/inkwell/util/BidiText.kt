package uk.ewancroft.inkwell.util

/** Prevents user-controlled text from reordering the surrounding UI label. */
fun String.bidiIsolated(): String = "\u2068$this\u2069"
