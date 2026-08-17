package uk.ewancroft.inkwell.ui.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.remote.VerificationResult
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.util.formatPublishedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uri: String,
    previousUri: String? = null,
    previousTitle: String? = null,
    nextUri: String? = null,
    nextTitle: String? = null,
    onBack: () -> Unit = {},
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val readerTheme = remember(uiState.documentTheme, uiState.publicationTheme, uiState.basicTheme, isDarkTheme) {
        ReaderTheme.resolve(
            documentTheme = uiState.documentTheme,
            publicationTheme = uiState.publicationTheme,
            basicTheme = uiState.basicTheme,
            isDarkTheme = isDarkTheme,
        )
    }

    LaunchedEffect(uri) {
        viewModel.loadPost(uri)
    }

    LaunchedEffect(previousUri, previousTitle, nextUri, nextTitle) {
        viewModel.setPreviousNext(previousUri, previousTitle, nextUri, nextTitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.title ?: "Post",
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val canonicalUrl = remember(uiState.publicationUrl, uiState.path, uiState.publicationUri) {
                        buildCanonicalUrl(uiState.publicationUrl, uiState.path)
                    }
                    if (canonicalUrl != null) {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, canonicalUrl)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                        }) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = "Share",
                            )
                        }
                        IconButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(canonicalUrl)))
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open in browser",
                            )
                        }
                    }
                },
            )
        },
        containerColor = if (readerTheme.background != Color.Unspecified) readerTheme.background else MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))
            uiState.loadError != null -> ErrorState(
                message = uiState.loadError!!,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.loadPost(uri, forceRefresh = true) },
            )
            else -> PostDetailContent(
                uiState = uiState,
                readerTheme = readerTheme,
                onToggleSubscription = { viewModel.toggleSubscription() },
                onToggleRecommend = { viewModel.toggleRecommend() },
                previousUri = uiState.previousUri,
                previousTitle = uiState.previousTitle,
                nextUri = uiState.nextUri,
                nextTitle = uiState.nextTitle,
                onNavigateToPost = onNavigateToPost,
                onNewCommentTextChanged = { viewModel.onNewCommentTextChanged(it) },
                onSubmitComment = { viewModel.submitComment() },
                onSetReplyTo = { viewModel.setReplyTo(it) },
                pollData = viewModel.pollData,
                onLoadPoll = { pollRef -> viewModel.loadPoll(pollRef) },
                onCastVote = { pollUri, options -> viewModel.castVote(pollUri, options) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (uiState.subscriptionError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissSubscriptionError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.subscriptionError!!)
        }
    }

    if (uiState.recommendError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissRecommendError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.recommendError!!)
        }
    }

    if (uiState.commentError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissCommentError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.commentError!!)
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                "Couldn't load this post",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
private fun PostDetailContent(
    uiState: PostDetailUiState,
    readerTheme: ReaderTheme,
    onToggleSubscription: () -> Unit,
    onToggleRecommend: () -> Unit,
    previousUri: String?,
    previousTitle: String?,
    nextUri: String?,
    nextTitle: String?,
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit,
    onNewCommentTextChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onSetReplyTo: (CommentEntry?) -> Unit,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>>,
    onLoadPoll: suspend (StrongRef) -> Unit,
    onCastVote: suspend (String, List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor = if (readerTheme.foreground != Color.Unspecified) readerTheme.foreground else MaterialTheme.colorScheme.onBackground
    val bodyColor = if (readerTheme.foreground != Color.Unspecified) readerTheme.foreground else MaterialTheme.colorScheme.onSurfaceVariant

    val pageBg = if (readerTheme.showPageBackground && readerTheme.pageBackground != Color.Unspecified) {
        readerTheme.pageBackground
    } else {
        Color.Unspecified
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .then(if (pageBg != Color.Unspecified) Modifier.background(pageBg) else Modifier),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (uiState.coverUrl != null) {
            item {
                AsyncImage(
                    model = uiState.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    uiState.title ?: "Untitled",
                    style = MaterialTheme.typography.headlineMedium,
                    color = titleColor,
                )
                if (!uiState.description.isNullOrBlank()) {
                    Text(
                        uiState.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                    )
                }
                if (!uiState.publishedAt.isNullOrBlank()) {
                    Text(
                        uiState.publishedAt.formatPublishedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = bodyColor.copy(alpha = 0.7f),
                    )
                }
                VerificationBadge(uiState.verification)
            }
        }

        item { HorizontalDivider(color = if (readerTheme.foreground != Color.Unspecified) readerTheme.foreground.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outlineVariant) }

        when (val content = uiState.content) {
            is DocumentContent.Leaflet -> {
                content.pages.forEach { page ->
                    page.blocks?.forEach { container ->
                        item {
                            LeafletBlockContent(
                                block = container.block,
                                authorDid = content.authorDid,
                                onLoadPoll = onLoadPoll,
                                onCastVote = onCastVote,
                            )
                        }
                    }
                }
            }

            is DocumentContent.PlainText -> {
                items(splitIntoParagraphs(content.text)) { paragraph ->
                    PlainTextParagraph(paragraph, foregroundColor = bodyColor)
                }
            }

            is DocumentContent.Unsupported -> {
                item { UnsupportedFormatNotice(content.formatType) }
            }

            DocumentContent.Empty -> {
                item { EmptyContentNotice() }
            }
        }

        item {
            HorizontalDivider()
            if (uiState.publicationUri != null) {
                SubscribeRow(uiState = uiState, onToggleSubscription = onToggleSubscription)
            }
            RecommendRow(uiState = uiState, onToggleRecommend = onToggleRecommend)
        }

        item {
            HorizontalDivider()
            CommentsSection(
                uiState = uiState,
                onTextChanged = onNewCommentTextChanged,
                onSubmitComment = onSubmitComment,
                onSetReplyTo = onSetReplyTo,
            )
        }

        item {
            PrevNextRow(
                previousUri = previousUri,
                previousTitle = previousTitle,
                nextUri = nextUri,
                nextTitle = nextTitle,
                onNavigateToPost = onNavigateToPost,
            )
        }
    }
}

@Composable
private fun PrevNextRow(
    previousUri: String?,
    previousTitle: String?,
    nextUri: String?,
    nextTitle: String?,
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit,
) {
    if (previousUri == null && nextUri == null) return

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (previousUri != null) {
            OutlinedButton(
                onClick = { onNavigateToPost(previousUri, null, null, null, null) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    previousTitle ?: "Previous",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (nextUri != null) {
            OutlinedButton(
                onClick = { onNavigateToPost(nextUri, null, null, null, null) },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    nextTitle ?: "Next",
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CommentsSection(
    uiState: PostDetailUiState,
    onTextChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onSetReplyTo: (CommentEntry?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Comments", style = MaterialTheme.typography.titleMedium)

        if (uiState.replyToComment != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Replying to \"${uiState.replyToComment.record.plaintext.take(40)}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onSetReplyTo(null) }) { Text("Cancel") }
            }
        }

        if (uiState.isLoadingComments) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (uiState.comments.isEmpty()) {
            Text(
                "No comments yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            uiState.comments.forEach { comment ->
                CommentRow(
                    comment = comment,
                    isReplyTo = uiState.replyToComment?.uri == comment.uri,
                    onReply = { onSetReplyTo(comment) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.newCommentText,
                onValueChange = onTextChanged,
                label = { Text(if (uiState.replyToComment != null) "Reply..." else "Add a comment...") },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSubmittingComment,
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSubmitComment,
                enabled = uiState.newCommentText.isNotBlank() && !uiState.isSubmittingComment,
            ) {
                if (uiState.isSubmittingComment) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Send comment")
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentEntry, isReplyTo: Boolean, onReply: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val annotated = buildAnnotatedString(comment.record.plaintext, comment.record.facets)
        Text(
            annotated,
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                comment.record.createdAt?.formatPublishedDate() ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = onReply, contentPadding = PaddingValues(0.dp)) {
                Text(if (isReplyTo) "Replying" else "Reply", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun VerificationBadge(result: VerificationResult?) {
    when (result) {
        null -> Unit

        is VerificationResult.Verified -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Verified source",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is VerificationResult.Failed -> {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Unverified source",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    result.failure.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun RecommendRow(uiState: PostDetailUiState, onToggleRecommend: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.isTogglingRecommend) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            IconToggleButton(
                checked = uiState.isRecommended,
                onCheckedChange = { onToggleRecommend() },
            ) {
                Icon(
                    if (uiState.isRecommended) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = if (uiState.isRecommended) "Unrecommend" else "Recommend",
                    tint = if (uiState.isRecommended) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isLoadingRecommendState) {
            Text(
                "Loading recommendations…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                when (uiState.recommendCount) {
                    0 -> "No recommendations yet"
                    1 -> "1 recommendation"
                    else -> "${uiState.recommendCount} recommendations"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubscribeRow(uiState: PostDetailUiState, onToggleSubscription: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.isTogglingSubscription) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            IconToggleButton(
                checked = uiState.isSubscribed,
                onCheckedChange = { onToggleSubscription() },
            ) {
                Icon(
                    if (uiState.isSubscribed) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    contentDescription = if (uiState.isSubscribed) "Unsubscribe" else "Subscribe",
                    tint = if (uiState.isSubscribed) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isLoadingSubscriptionState) {
            Text(
                "Loading subscription…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                if (uiState.isSubscribed) "Subscribed" else "Subscribe to publication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun splitIntoParagraphs(text: String): List<String> =
    text.split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

@Composable
private fun PlainTextParagraph(paragraph: String, foregroundColor: Color = Color.Unspecified) {
    val headingLevel = paragraph.takeWhile { it == '#' }.length.coerceAtMost(6)
    val color = if (foregroundColor != Color.Unspecified) foregroundColor else MaterialTheme.colorScheme.onBackground
    if (headingLevel in 1..6 && paragraph.getOrNull(headingLevel) == ' ') {
        val text = paragraph.drop(headingLevel + 1).trim()
        val style = when (headingLevel) {
            1 -> MaterialTheme.typography.headlineSmall
            2 -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.titleMedium
        }
        Text(text, style = style, color = color, modifier = Modifier.fillMaxWidth())
    } else {
        Text(
            paragraph,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun UnsupportedFormatNotice(formatType: String?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.HourglassEmpty, contentDescription = null)
            Column {
                Text("Unsupported content format", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (formatType != null) "This post uses \"$formatType\", which Inkwell doesn't render yet."
                    else "This post's content couldn't be identified.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyContentNotice() {
    Text(
        "This post doesn't have any content yet.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun buildCanonicalUrl(baseUrl: String?, path: String?): String? {
    if (baseUrl == null) return null
    val trimmedBase = baseUrl.trimEnd('/')
    val trimmedPath = path?.trimStart('/')?.trimEnd('/') ?: return trimmedBase
    return if (trimmedPath.isEmpty()) trimmedBase else "$trimmedBase/$trimmedPath"
}
