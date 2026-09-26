package uk.ewancroft.inkwell.deeplink

import android.content.Intent
import android.net.Uri
import uk.ewancroft.inkwell.shared.AtUri
import uk.ewancroft.inkwell.shared.graph.CollectionNsids

/**
 * Recognizes Inkwell's HTTPS universal/App Link hand-off
 * (`https://inkwell.ewancroft.uk/open?uri=<AT-URI>`) — separate from both the trusted
 * `inkwell://document` custom scheme ([ContentDeepLinkPolicy] in `MainActivity.kt`) and the
 * OAuth callback intent-filter.
 *
 * A URI arriving here is only an *unverified claim* about a document AT-URI. Unlike the
 * custom scheme — used for internal routing/notifications, where Inkwell itself is the
 * source of the AT-URI — an `https://` link can be authored and shared by anyone,
 * including a link claiming an AT-URI the domain never actually published.
 * [HttpsDeepLinkResolver] must confirm the claim against the author's own PDS record and
 * published discovery link before it's safe to route into the Reader.
 */
internal object HttpsDeepLinkPolicy {
    private const val HOST = "inkwell.ewancroft.uk"
    private const val PATH_PREFIX = "/open"

    /** Extracts the unverified candidate document AT-URI from an incoming https intent, or null. */
    fun candidateDocumentUri(intent: Intent): String? = intent.data?.let(::candidateDocumentUri)

    fun candidateDocumentUri(data: Uri): String? {
        if (!data.scheme.equals("https", ignoreCase = true)) return null
        if (!data.host.equals(HOST, ignoreCase = true)) return null
        val path = data.path.orEmpty()
        if (path != PATH_PREFIX && !path.startsWith("$PATH_PREFIX/")) return null
        val raw = data.getQueryParameter("uri") ?: return null
        val parsed = AtUri.parse(raw) ?: return null
        return raw.takeIf { parsed.collection == CollectionNsids.DOCUMENT }
    }
}
