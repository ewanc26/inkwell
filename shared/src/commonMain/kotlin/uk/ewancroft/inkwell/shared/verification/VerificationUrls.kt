package uk.ewancroft.inkwell.shared.verification

object VerificationUrls {

    /**
     * Builds the `.well-known` verification endpoint for a publication, including the
     * publication's own path for non-root publications — e.g. a publication living at
     * `https://example.com/writing` verifies at
     * `https://example.com/.well-known/site.standard.publication/writing`.
     */
    fun publicationVerificationUrl(publicationUrl: String): String? {
        val url = normalizeHttpsUrl(publicationUrl) ?: return null
        val publicationPath = url.path.trim('/')
        val basePath = "/.well-known/site.standard.publication"
        val fullPath = if (publicationPath.isEmpty()) basePath else "$basePath/$publicationPath"
        return "${url.scheme}://${url.host}$fullPath"
    }

    /**
     * Builds the canonical web URL for a document per standard.site's `site` + `path`
     * rules. A resolved publication URL is required when `documentSite` is an AT-URI
     * (i.e. the document belongs to a publication) rather than a direct `https://` URL.
     */
    fun documentCanonicalUrl(documentSite: String, documentPath: String?, publicationUrl: String?): String? {
        val baseString = if (documentSite.startsWith("at://")) {
            publicationUrl ?: return null
        } else {
            documentSite
        }
        val base = normalizeHttpsUrl(baseString) ?: return null
        val path = documentPath?.trim('/') ?: return base.toString()
        val basePath = base.path.trim('/')
        val fullPath = listOf(basePath, path).filter { it.isNotEmpty() }.joinToString("/")
        return "${base.scheme}://${base.host}/$fullPath"
    }

    /**
     * Builds the `<link>` discovery tag a document page must serve in its
     * `<head>` to point back at its AT-URI record.
     */
    fun discoveryLinkTag(recordURI: String, relation: String): String =
        "<link rel=\"$relation\" href=\"$recordURI\" />"

    private data class UrlParts(val scheme: String, val host: String, val path: String)

    private fun normalizeHttpsUrl(urlString: String): UrlParts? {
        val trimmed = urlString.trim()
        val lowerScheme = trimmed.substringBefore("://").lowercase()
        val afterScheme = trimmed.substringAfter("://", "")
        if (lowerScheme != "https" || afterScheme.isEmpty()) return null
        val host = afterScheme.substringBefore("/", "").substringBefore("?", "").substringBefore("#", "")
        if (host.isEmpty()) return null
        val path = afterScheme.substringAfter(host, "")
        return UrlParts(scheme = "https", host = host, path = path)
    }
}
