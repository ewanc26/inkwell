package uk.ewancroft.inkwell.ui.reader

import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.content.LeafletPage
import uk.ewancroft.inkwell.shared.verification.VerificationResult

sealed class DocumentContent {
    data class Leaflet(val pages: List<LeafletPage>, val authorDid: String) : DocumentContent()
    data class Markdown(val text: String) : DocumentContent()
    data class PlainText(val text: String) : DocumentContent()
    data object Empty : DocumentContent()
    data class Unsupported(val formatType: String?) : DocumentContent()
}

data class PostDetailUiState(
    val uri: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val title: String? = null,
    val description: String? = null,
    val publishedAt: String? = null,
    val path: String? = null,
    val coverUrl: String? = null,
    val content: DocumentContent = DocumentContent.Empty,
    val lostContent: List<String> = emptyList(),
    val publicationUri: String? = null,
    val publicationUrl: String? = null,

    val documentTheme: PublicationTheme? = null,
    val publicationTheme: PublicationTheme? = null,
    val basicTheme: BasicTheme? = null,

    val verification: VerificationResult? = null,

    val isSubscribed: Boolean = false,
    val subscriptionRkey: String? = null,
    val isLoadingSubscriptionState: Boolean = false,
    val hasLoadedSubscriptionState: Boolean = false,
    val isTogglingSubscription: Boolean = false,
    val subscriptionError: String? = null,

    val isRecommended: Boolean = false,
    val recommendRkey: String? = null,
    val recommendCount: Int = 0,
    val isLoadingRecommendState: Boolean = false,
    val hasLoadedRecommendState: Boolean = false,
    val isTogglingRecommend: Boolean = false,
    val recommendError: String? = null,

    val comments: List<CommentEntry> = emptyList(),
    val newCommentText: String = "",
    val isSubmittingComment: Boolean = false,
    val isLoadingComments: Boolean = false,
    val replyToComment: CommentEntry? = null,
    val commentError: String? = null,

    val previousUri: String? = null,
    val previousTitle: String? = null,
    val nextUri: String? = null,
    val nextTitle: String? = null,
)

data class CommentEntry(
    val uri: String,
    val recordKey: String,
    val record: uk.ewancroft.inkwell.data.model.graph.LeafletComment,
)
