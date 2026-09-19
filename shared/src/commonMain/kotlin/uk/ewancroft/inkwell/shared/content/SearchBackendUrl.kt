package uk.ewancroft.inkwell.shared.content

object SearchBackendUrl {
    const val BASE = "https://leaflet-search-backend.fly.dev"
    const val PUBLIC_APPVIEW = "https://public.api.bsky.app"

    /** Returns keyword-indexed documents/articles. */
    const val KEYWORD_MODE = "keyword"

    /**
     * Retained for source compatibility only. The backend does not support a
     * publications search mode; clients must filter native publication
     * results returned by the documented keyword/semantic/hybrid modes.
     */
    @Deprecated("The pub-search API has no publications mode")
    const val PUBLICATIONS_MODE = "publications"

}
