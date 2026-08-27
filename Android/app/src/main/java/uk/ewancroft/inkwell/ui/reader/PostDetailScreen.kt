package uk.ewancroft.inkwell.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import uk.ewancroft.inkwell.shared.text.StringUtils
import uk.ewancroft.inkwell.ui.moderation.ReportDialog

private data class PostReportTarget(
    val subject: String,
    val recordCid: String? = null,
)

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
    val isDarkTheme = uk.ewancroft.inkwell.ui.theme.LocalForceDarkTheme.current
        ?: androidx.compose.foundation.isSystemInDarkTheme()
    val context = androidx.compose.ui.platform.LocalContext.current
    var reportTarget by remember { mutableStateOf<PostReportTarget?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val readerTheme = remember(uiState.documentTheme, uiState.publicationTheme, uiState.basicTheme, isDarkTheme) {
        ReaderTheme.resolve(
            documentTheme = uiState.documentTheme,
            publicationTheme = uiState.publicationTheme,
            basicTheme = uiState.basicTheme,
            isDarkTheme = isDarkTheme,
            overrideAccentRgb = uk.ewancroft.inkwell.util.CustomisationPreferences.getAccentColorRgbInt(context),
            overrideFontFamily = uk.ewancroft.inkwell.util.CustomisationPreferences.getFontFamilyOverride(context),
            increaseContrast = uk.ewancroft.inkwell.util.AccessibilityPreferences.getIncreaseContrast(context),
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
                            uk.ewancroft.inkwell.util.LinkPreferences.openContentUrl(context, canonicalUrl)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open in browser",
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report post") },
                                onClick = {
                                    showMoreMenu = false
                                    reportTarget = PostReportTarget(uri, uiState.recordCid)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Report, contentDescription = null)
                                },
                            )
                            uiState.authorDid?.takeIf(String::isNotBlank)?.let { authorDid ->
                                DropdownMenuItem(
                                    text = { Text("Report account") },
                                    onClick = {
                                        showMoreMenu = false
                                        reportTarget = PostReportTarget(authorDid)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Report, contentDescription = null)
                                    },
                                )
                            }
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
                onToggleBookmark = { viewModel.toggleBookmark() },
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

    reportTarget?.let { target ->
        ReportDialog(
            subject = target.subject,
            onDismiss = { reportTarget = null },
            onSubmit = { reason, comment ->
                reportTarget = null
                viewModel.submitReport(
                    subject = target.subject,
                    recordCid = target.recordCid,
                    reasonType = reason,
                    reason = comment,
                )
            },
        )
    }

    if (uiState.reportError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissReportError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.reportError!!)
        }
    }

    if (uiState.reportConfirmation != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.dismissReportConfirmation() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.reportConfirmation!!)
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

private fun buildCanonicalUrl(baseUrl: String?, path: String?): String? {
    if (baseUrl == null) return null
    val trimmedBase = StringUtils.trimTrailingSlash(baseUrl)
    val trimmedPath = path?.trimStart('/')?.trimEnd('/') ?: return trimmedBase
    return if (trimmedPath.isEmpty()) trimmedBase else "$trimmedBase/$trimmedPath"
}
