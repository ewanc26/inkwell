package uk.ewancroft.inkwell.ui.reader

import android.net.Uri

internal object IframeSecurityPolicy {
    fun isAllowedInitial(uri: Uri): Boolean =
        uri.scheme == "https" && !uri.host.isNullOrBlank()

    fun isAllowedNavigation(originHost: String?, candidate: Uri): Boolean =
        candidate.scheme == "https" && candidate.host == originHost
}
