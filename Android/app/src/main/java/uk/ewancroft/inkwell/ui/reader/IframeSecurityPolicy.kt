package uk.ewancroft.inkwell.ui.reader

import android.net.Uri

internal object IframeSecurityPolicy {
    fun isAllowedInitial(uri: Uri): Boolean =
        uri.scheme == "https" && !uri.host.isNullOrBlank()

    fun isAllowedNavigation(origin: Uri, candidate: Uri): Boolean =
        candidate.scheme == "https" &&
            candidate.host == origin.host &&
            effectivePort(candidate) == effectivePort(origin)

    private fun effectivePort(uri: Uri): Int = uri.port.takeIf { it != -1 } ?: 443
}
