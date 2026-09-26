package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
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

@Serializable
sealed class BSkyEmbed {
    @Serializable
    @SerialName(BlueskyEmbedTypes.IMAGES)
    data class Images(val images: List<BSkyImage>) : BSkyEmbed()

    @Serializable
    @SerialName(BlueskyEmbedTypes.EXTERNAL)
    data class External(val external: BSkyExternal) : BSkyEmbed()

    @Serializable
    @SerialName(BlueskyEmbedTypes.RECORD)
    data class Record(val record: BSkyEmbeddedRecord) : BSkyEmbed()

    @Serializable
    data object Unknown : BSkyEmbed()
}

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

@Serializable
data class GetPostsResponse(
    val posts: List<BSkyPostView>
)
