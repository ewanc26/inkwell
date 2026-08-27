package uk.ewancroft.inkwell.shared.content

object SearchBackendUrl {
    const val BASE = "https://leaflet-search-backend.fly.dev"
    const val PUBLIC_APPVIEW = "https://public.api.bsky.app"

    /** Returns keyword-indexed documents/articles. */
    const val KEYWORD_MODE = "keyword"

    /** Returns `type=publication` records only. */
    const val PUBLICATIONS_MODE = "publications"
}
