package uk.ewancroft.inkwell.shared.content

object BlueskyEmbedTypes {
    const val IMAGES = "app.bsky.embed.images"
    const val EXTERNAL = "app.bsky.embed.external"
    const val RECORD = "app.bsky.embed.record"
    const val VIDEO = "app.bsky.embed.video"
    const val RECORD_WITH_MEDIA = "app.bsky.embed.recordWithMedia"

    /**
     * The `#view` variants, as served by `app.bsky.feed.getPosts`. Views
     * never use the bare record IDs, so decoders must match these — matching
     * the bare IDs silently drops (iOS) or hard-fails (Android) every embed.
     */
    const val IMAGES_VIEW = "$IMAGES#view"
    const val EXTERNAL_VIEW = "$EXTERNAL#view"
    const val RECORD_VIEW = "$RECORD#view"
    const val VIDEO_VIEW = "$VIDEO#view"
    const val RECORD_WITH_MEDIA_VIEW = "$RECORD_WITH_MEDIA#view"
}
