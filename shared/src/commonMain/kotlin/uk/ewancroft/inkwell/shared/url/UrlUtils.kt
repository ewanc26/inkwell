package uk.ewancroft.inkwell.shared.url

object UrlUtils {

    /**
     * Normalizes a URL: lowercases scheme and host, trims trailing slashes.
     * Returns the original string if it can't be parsed as a URL.
     */
    fun normalizedSite(value: String): String {
        val trimmed = value.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd < 0) return trimmed.trimEnd('/')
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        val afterScheme = trimmed.substring(schemeEnd + 3)
        val hostEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        val host = if (hostEnd >= 0) afterScheme.substring(0, hostEnd).lowercase() else afterScheme.lowercase()
        val path = if (hostEnd >= 0) afterScheme.substring(hostEnd) else ""
        return "$scheme://$host$path".trimEnd('/')
    }

    /**
     * Builds the canonical web URL described by Standard.site's `site` + `path`
     * rules. Returns null if `site` is not a valid HTTPS URL and no publication
     * URL is provided for AT-URI sites.
     */
    fun canonicalUrl(site: String, path: String?, publicationUrl: String? = null): String? {
        val baseString = if (site.startsWith("at://")) {
            publicationUrl ?: return null
        } else {
            site
        }
        val trimmed = baseString.trim()
        val schemeEnd = trimmed.indexOf("://")
        if (schemeEnd < 0) return null
        val scheme = trimmed.substring(0, schemeEnd).lowercase()
        if (scheme != "https") return null
        val afterScheme = trimmed.substring(schemeEnd + 3)
        val hostEnd = afterScheme.indexOfAny(charArrayOf('/', '?', '#'))
        val host = (if (hostEnd >= 0) afterScheme.substring(0, hostEnd) else afterScheme).ifEmpty { return null }
        val basePath = (if (hostEnd >= 0) afterScheme.substring(hostEnd) else "").trim('/')
        val docPath = path?.trim('/') ?: return "https://$host/${basePath.trim('/')}".trimEnd('/')
        val fullPath = listOf(basePath, docPath).filter { it.isNotEmpty() }.joinToString("/")
        return "https://$host/$fullPath"
    }
}
