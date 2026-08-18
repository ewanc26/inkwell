package uk.ewancroft.inkwell.shared.content

object CdnUrls {
    fun bskyThumbnail(did: String, link: String): String =
        "https://cdn.bsky.app/img/feed_thumbnail/plain/$did/$link"
}
