package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.Serializable

@Serializable
data class SearchResult(
    val type: String,
    val uri: String,
    val did: String,
    val title: String,
    val snippet: String? = null,
    val createdAt: String? = null,
    val rkey: String? = null,
    val basePath: String? = null,
    val platform: String? = null,
    val path: String? = null,
    val coverImage: String? = null,
    val handle: String? = null
) {
    val isPublication: Boolean get() = type == "publication"

    val isStandardSiteDocument: Boolean get() =
        AtUri.parse(uri)?.collection == "site.standard.document"
}

@Serializable
data class SearchResponse(
    val results: List<SearchResult>,
    val total: Int? = null,
    val hasMore: Boolean = false
)
