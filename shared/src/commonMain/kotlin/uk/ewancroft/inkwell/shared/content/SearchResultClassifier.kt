package uk.ewancroft.inkwell.shared.content

import uk.ewancroft.inkwell.shared.AtUri

/**
 * Shared classification and URL construction for search/discovery results.
 *
 * Both platforms use the same logic to determine whether a search result
 * is a publication or a Standard.site document, and to construct the
 * canonical web URL for navigation.
 */
object SearchResultClassifier {

    const val PUBLICATION_TYPE = "publication"
    const val SITE_STANDARD_DOCUMENT = "site.standard.document"
    const val LEAFLET_PLATFORM = "leaflet"

    /**
     * Returns true if [type] indicates a publication record.
     */
    fun isPublication(type: String): Boolean = type == PUBLICATION_TYPE

    /**
     * Returns true if [uri] is a `site.standard.document` record.
     */
    fun isStandardSiteDocument(uri: String): Boolean {
        val parsed = AtUri.parse(uri)
        return parsed?.collection == SITE_STANDARD_DOCUMENT
    }

    /**
     * Constructs the canonical web URL for a search result.
     *
     * Returns null if [basePath] is null or empty.
     *
     * - Publications link to the origin directly.
     * - Documents with a [path] link to `origin + path`.
     * - Leaflet documents without a path fall back to `origin + rkey`.
     */
    fun webURL(
        basePath: String?,
        path: String?,
        rkey: String?,
        platform: String?,
        isPublication: Boolean,
    ): String? {
        if (basePath.isNullOrEmpty()) return null
        val origin = if (basePath.startsWith("http")) basePath else "https://$basePath"
        if (isPublication) return origin
        if (!path.isNullOrEmpty()) {
            return if (path.startsWith("/")) origin + path else "$origin/$path"
        }
        if (platform == LEAFLET_PLATFORM && !rkey.isNullOrEmpty()) {
            return "$origin/$rkey"
        }
        return null
    }
}
