package uk.ewancroft.inkwell.data.model.common

import kotlinx.serialization.Serializable
import uk.ewancroft.inkwell.shared.content.SearchResultClassifier

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
    val isPublication: Boolean get() = SearchResultClassifier.isPublication(type)

    val isStandardSiteDocument: Boolean get() =
        SearchResultClassifier.isStandardSiteDocument(uri)

    fun webURL(): String? =
        SearchResultClassifier.webURL(basePath, path, rkey, platform, isPublication)
}

@Serializable
data class SearchActorResult(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val avatar: String? = null,
)

@Serializable
data class SearchActorResponse(
    val actors: List<SearchActorResult> = emptyList(),
)

@Serializable
data class SearchResponse(
    val results: List<SearchResult> = emptyList(),
    val total: Int? = null,
    val hasMore: Boolean = false,
)

/**
 * An authoritative publication hydrated from a native publication search
 * result. The search result's AT-URI is retained as the identity; display
 * metadata comes from the owning PDS record rather than from an article
 * snapshot or inferred domain grouping.
 */
data class PublicationResult(
    val uri: String,
    val name: String,
    val domain: String,
    val url: String,
    val did: String,
    val coverImage: String?,
) {
    val isSubscribable: Boolean get() = false
}
