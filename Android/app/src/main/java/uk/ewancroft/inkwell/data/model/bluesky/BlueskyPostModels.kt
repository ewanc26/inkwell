package uk.ewancroft.inkwell.data.model.bluesky

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

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
    @SerialName("app.bsky.embed.images")
    data class Images(val images: List<BSkyImage>) : BSkyEmbed()

    @Serializable
    @SerialName("app.bsky.embed.external")
    data class External(val external: BSkyExternal) : BSkyEmbed()

    @Serializable
    @SerialName("app.bsky.embed.record")
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

@Serializable
data class BSkyExternal(
    val uri: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumb: String? = null,
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
