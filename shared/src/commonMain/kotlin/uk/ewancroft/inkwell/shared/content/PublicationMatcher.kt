package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.url.UrlUtils

/**
 * Shared publication/document matching logic.
 *
 * Determines whether a document belongs to a publication by comparing
 * the document's site against the publication's AT-URI and URL.
 */
object PublicationMatcher {

    /**
     * Returns true if [documentSite] belongs to the publication identified by
     * [publicationUri] and [publicationUrl].
     *
     * Matches on:
     * - Exact AT-URI equality: `documentSite == publicationUri`
     * - Normalized URL equality: handles case differences and trailing slashes
     * - Subpath prefix: `documentSite` starts with `publicationUrl/`
     */
    fun documentBelongsToPublication(
        documentSite: String,
        publicationUri: String,
        publicationUrl: String?
    ): Boolean {
        if (documentSite == publicationUri) return true
        val pubUrl = publicationUrl ?: return false
        val normalizedDoc = UrlUtils.normalizedSite(documentSite)
        val normalizedPub = UrlUtils.normalizedSite(pubUrl)
        if (normalizedDoc == normalizedPub) return true
        return normalizedDoc.startsWith("$normalizedPub/")
    }
}
