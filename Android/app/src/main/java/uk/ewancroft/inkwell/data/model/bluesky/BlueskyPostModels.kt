package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.shared.content.BlueskyEmbedTypes

/**
 * Bluesky post view models for embedded posts.
 * Used by app.bsky.feed.getPosts to fetch and render Bluesky post embeds.
 */

@Serializable
data class BSkyPostView(
    val uri: String,
    val cid: String? = null,
    val author: BSkyAuthor,
    val record: BSkyPostRecord,
    val replyCount: Int? = null,
    val repostCount: Int? = null,
    val likeCount: Int? = null,
    val embed: BSkyEmbed? = null,
)

@Serializable
data class BSkyAuthor(
    val did: String? = null,
    val handle: String? = null,
    val displayName: String? = null,
    val avatar: String? = null,
)

@Serializable
data class BSkyPostRecord(
    val text: String? = null,
    val createdAt: String? = null,
)

/**
 * Union of possible Bluesky post embeds, selected on the wire `$type` —
 * which is always a `#view` variant, never a bare record ID. Unknown
 * present and future embed types decode as [Unknown], so one new embed kind
 * can never sink the whole post the way the old base-ID sealed mismatch did.
 */
@Serializable(with = BSkyEmbedSerializer::class)
sealed class BSkyEmbed {
    @Serializable
    data class Images(val images: List<BSkyImage>) : BSkyEmbed()

    @Serializable
    data class External(val external: BSkyExternal) : BSkyEmbed()

    @Serializable
    data class Record(val record: BSkyEmbeddedRecord) : BSkyEmbed()

    /**
     * `app.bsky.embed.video#view`, whose fields sit flat on the view (there
     * is no `video` wrapper object). Decoded so video posts survive the
     * fetch; playback UI is a follow-up — today these render as unadorned posts.
     */
    @Serializable
    data class Video(
        val cid: String? = null,
        val playlist: String? = null,
        val thumbnail: String? = null,
        val alt: String? = null,
    ) : BSkyEmbed()

    @Serializable
    data class RecordWithMedia(
        val record: BSkyRecordWithMediaRecord? = null,
        val media: BSkyEmbedMedia? = null,
    ) : BSkyEmbed()

    @Serializable
    data object Unknown : BSkyEmbed()
}

object BSkyEmbedSerializer : JsonContentPolymorphicSerializer<BSkyEmbed>(BSkyEmbed::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BSkyEmbed> =
        when (embedTypeOf(element)) {
            BlueskyEmbedTypes.IMAGES_VIEW -> BSkyEmbed.Images.serializer()
            BlueskyEmbedTypes.EXTERNAL_VIEW -> BSkyEmbed.External.serializer()
            BlueskyEmbedTypes.RECORD_VIEW -> BSkyEmbed.Record.serializer()
            BlueskyEmbedTypes.VIDEO_VIEW -> BSkyEmbed.Video.serializer()
            BlueskyEmbedTypes.RECORD_WITH_MEDIA_VIEW -> BSkyEmbed.RecordWithMedia.serializer()
            else -> BSkyEmbed.Unknown.serializer()
        }
}

/**
 * Media union nested inside `recordWithMedia#view` (images or external only,
 * per the lexicon). Same `$type` selection as the top level, same
 * never-fail fallback.
 */
@Serializable(with = BSkyEmbedMediaSerializer::class)
sealed class BSkyEmbedMedia {
    @Serializable
    data class Images(val images: List<BSkyImage>) : BSkyEmbedMedia()

    @Serializable
    data class External(val external: BSkyExternal) : BSkyEmbedMedia()

    @Serializable
    data object Unknown : BSkyEmbedMedia()
}

object BSkyEmbedMediaSerializer : JsonContentPolymorphicSerializer<BSkyEmbedMedia>(BSkyEmbedMedia::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<BSkyEmbedMedia> =
        when (embedTypeOf(element)) {
            BlueskyEmbedTypes.IMAGES_VIEW -> BSkyEmbedMedia.Images.serializer()
            BlueskyEmbedTypes.EXTERNAL_VIEW -> BSkyEmbedMedia.External.serializer()
            else -> BSkyEmbedMedia.Unknown.serializer()
        }
}

private fun embedTypeOf(element: JsonElement): String? =
    (element as? JsonObject)?.get("\$type")?.jsonPrimitive?.contentOrNull

@Serializable
data class BSkyImage(
    val thumb: String? = null,
    val fullsize: String? = null,
    val alt: String? = null,
)

/**
 * An `app.bsky.embed.external#viewExternal` view. Bluesky's May 2026
 * Standard.site integration enriches this shape with [readingTime],
 * [labels], [source], [associatedRefs], and [associatedProfiles] on
 * external links backed by a standard.site publication. Plain Bluesky
 * external cards simply omit all of these, so every enriched field is
 * nullable and decoding degrades gracefully either way (the fetcher's
 * `Json { ignoreUnknownKeys = true }` config also means fields this class
 * doesn't yet model won't break decoding).
 */
@Serializable
data class BSkyExternal(
    val uri: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumb: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val readingTime: Int? = null,
    val labels: List<BSkyLabel>? = null,
    val source: BSkyExternalSource? = null,
    val associatedRefs: List<BSkyStrongRef>? = null,
    val associatedProfiles: List<BSkyAuthor>? = null,
)

@Serializable
data class BSkyStrongRef(
    val uri: String? = null,
    val cid: String? = null,
)

/**
 * `com.atproto.label.defs#label` — only the fields Inkwell surfaces (the
 * short value and whether it's a negation) are decoded; the rest of the
 * label envelope isn't currently rendered.
 */
@Serializable
data class BSkyLabel(
    val src: String? = null,
    val uri: String? = null,
    @SerialName("val") val value: String? = null,
    val neg: Boolean? = null,
    val cts: String? = null,
)

/**
 * `app.bsky.embed.external#viewExternalSource` — identifies the
 * standard.site (or other) publication that backs an enriched external
 * embed.
 */
@Serializable
data class BSkyExternalSource(
    val uri: String? = null,
    val icon: String? = null,
    val title: String? = null,
    val description: String? = null,
    val theme: BSkyExternalSourceTheme? = null,
)

/** `app.bsky.embed.external#viewExternalSourceTheme`. */
@Serializable
data class BSkyExternalSourceTheme(
    val backgroundRGB: BSkyColorRGB? = null,
    val foregroundRGB: BSkyColorRGB? = null,
    val accentRGB: BSkyColorRGB? = null,
    val accentForegroundRGB: BSkyColorRGB? = null,
)

/** `app.bsky.embed.external#colorRGB`. */
@Serializable
data class BSkyColorRGB(
    val r: Int? = null,
    val g: Int? = null,
    val b: Int? = null,
)

@Serializable
data class BSkyEmbeddedRecord(
    val uri: String? = null,
    val cid: String? = null,
    val author: BSkyAuthor? = null,
    val value: BSkyEmbeddedRecordValue? = null,
)

@Serializable
data class BSkyEmbeddedRecordValue(
    val text: String? = null,
    val createdAt: String? = null,
)

/** `recordWithMedia#view`'s `record` member — itself a record-view union. */
@Serializable
data class BSkyRecordWithMediaRecord(
    val record: BSkyEmbeddedRecord? = null,
)

@Serializable
data class GetPostsResponse(
    val posts: List<BSkyPostView>
)
