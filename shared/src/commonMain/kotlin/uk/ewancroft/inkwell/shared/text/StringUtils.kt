package uk.ewancroft.inkwell.shared.text

object StringUtils {

    fun trimTrailingSlash(value: String): String =
        value.trimEnd('/')
}
