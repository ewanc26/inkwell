package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.util.formatPublishedDate

@Composable
internal fun CommentsSection(
    uiState: PostDetailUiState,
    onTextChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onSetReplyTo: (CommentEntry?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.reader_comments), style = MaterialTheme.typography.titleMedium)

        if (uiState.replyToComment != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.reader_replying_to, uiState.replyToComment.record.plaintext.take(40)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onSetReplyTo(null) }) { Text(stringResource(R.string.reader_cancel)) }
            }
        }

        if (uiState.isLoadingComments) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (uiState.comments.isEmpty()) {
            Text(
                stringResource(R.string.reader_no_comments),
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
                label = { Text(stringResource(if (uiState.replyToComment != null) R.string.reader_reply_hint else R.string.reader_comment_hint)) },
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
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.Send, contentDescription = stringResource(R.string.reader_send_comment))
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
