package uk.ewancroft.inkwell.shared.util

object HandleUtils {

    fun normalize(handle: String): String =
        handle.trim().lowercase().replace("@", "")
}
