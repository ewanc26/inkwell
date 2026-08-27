package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.shared.verification.VerificationResult
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.util.formatPublishedDate
import uk.ewancroft.inkwell.util.rememberInkwellHaptics

@Composable
internal fun PostDetailContent(
    uiState: PostDetailUiState,
    readerTheme: ReaderTheme,
    onToggleSubscription: () -> Unit,
    onToggleRecommend: () -> Unit,
    onToggleBookmark: () -> Unit,
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
    val accentColor = if (readerTheme.accent != Color.Unspecified) readerTheme.accent else MaterialTheme.colorScheme.primary
    val titleAccessibilityLabel = buildList {
        add(uiState.title ?: "Untitled")
        uiState.authorDid?.takeIf(String::isNotBlank)?.let { add("Author: $it") }
    }.joinToString(separator = ". ")

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
                    modifier = Modifier.semantics {
                        heading()
                        contentDescription = titleAccessibilityLabel
                    },
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
                                alignment = container.alignment,
                                onLoadPoll = onLoadPoll,
                                onCastVote = onCastVote,
                            )
                        }
                    }
                }
            }

            is DocumentContent.Markdown -> {
                item {
                    MarkdownRendererView(
                        markdown = content.text,
                        foregroundColor = bodyColor,
                        accentColor = accentColor,
                    )
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

        if (uiState.lostContent.isNotEmpty()) {
            item {
                LostContentBanner(uiState.lostContent)
            }
        }

        item {
            HorizontalDivider()
            if (uiState.publicationUri != null) {
                SubscribeRow(uiState = uiState, onToggleSubscription = onToggleSubscription)
            }
            RecommendRow(uiState = uiState, onToggleRecommend = onToggleRecommend)
            BookmarkRow(uiState = uiState, onToggleBookmark = onToggleBookmark)
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
private fun VerificationBadge(result: VerificationResult?) {
    when (result) {
        null -> Unit

        is VerificationResult.Verified -> {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Source verification: verified"
                },
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
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Source verification: unverified. ${result.failure.reason}"
                },
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
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
    val haptics = rememberInkwellHaptics()
    LaunchedEffect(uiState.isSubscribed) {
        if (uiState.isSubscribed) haptics.success()
    }
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
                onCheckedChange = {
                    haptics.light()
                    onToggleSubscription()
                },
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

@Composable
private fun BookmarkRow(uiState: PostDetailUiState, onToggleBookmark: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconToggleButton(
            checked = uiState.isBookmarked,
            onCheckedChange = { onToggleBookmark() },
        ) {
            Icon(
                if (uiState.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (uiState.isBookmarked) "Remove bookmark" else "Bookmark",
                tint = if (uiState.isBookmarked) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (uiState.isBookmarked) "Bookmarked" else "Bookmark this post",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
private fun LostContentBanner(lost: List<String>) {
    val label = when {
        lost.size == 1 -> "Some content couldn't be displayed (${lost[0]})."
        lost.size == 2 -> "Some content couldn't be displayed (${lost[0]} and ${lost[1]})."
        else -> {
            val allButLast = lost.dropLast(1).joinToString(", ")
            "Some content couldn't be displayed ($allButLast, and ${lost.last()})."
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
