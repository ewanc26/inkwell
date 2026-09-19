package uk.ewancroft.inkwell.shared.verification

import io.ktor.http.Url

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
        return "${url.scheme}://${url.authority}$fullPath"
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
        val path = documentPath?.trim('/')
            ?: return "${base.scheme}://${base.authority}/${base.path.trim('/')}".trimEnd('/')
        val basePath = base.path.trim('/')
        val fullPath = listOf(basePath, path).filter { it.isNotEmpty() }.joinToString("/")
        return "${base.scheme}://${base.authority}/$fullPath"
    }

    /**
     * Builds the `<link>` discovery tag a document page must serve in its
     * `<head>` to point back at its AT-URI record.
     */
    fun discoveryLinkTag(recordURI: String, relation: String): String =
        "<link rel=\"$relation\" href=\"$recordURI\" />"

    private data class UrlParts(
        val scheme: String,
        val host: String,
        val path: String,
        val port: Int,
    ) {
        val authority: String
            get() = if (port == 443) host else "$host:$port"
    }

    private fun normalizeHttpsUrl(urlString: String): UrlParts? {
        val trimmed = urlString.trim()
        val scheme = trimmed.substringBefore("://", "").lowercase()
        if (scheme != "https") return null
        val remainder = trimmed.substringAfter("://", "")
        if (remainder.isEmpty()) return null
        val authorityText = remainder.substringBefore('/').substringBefore('?').substringBefore('#')
        if ('@' in authorityText) return null
        val parsed = runCatching { Url("https://$remainder") }.getOrNull() ?: return null
        if (parsed.protocol.name.lowercase() != "https" || parsed.host.isEmpty()) return null
        return UrlParts(
            scheme = "https",
            host = parsed.host,
            path = parsed.encodedPath,
            port = parsed.port,
        )
    }
}
