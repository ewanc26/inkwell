package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

/**
 * Renders a single `site.standard.document` post: fetches the record from
 * its author's PDS, resolves the content format, and renders it.
 *
 * Comments and interactions (likes, reposts, replies) are intentionally
 * out of scope here — this screen renders document *content* only. See the
 * note at the bottom of the screen and in AGENTS.md's capability gaps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uri: String,
    onBack: () -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uri) {
        viewModel.loadPost(uri)
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
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                modifier = Modifier.padding(padding),
                onRetry = { viewModel.loadPost(uri, forceRefresh = true) },
            )
            else -> PostDetailContent(uiState = uiState, modifier = Modifier.padding(padding))
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
private fun PostDetailContent(uiState: PostDetailUiState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (!uiState.description.isNullOrBlank()) {
                    Text(
                        uiState.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!uiState.publishedAt.isNullOrBlank()) {
                    Text(
                        uiState.publishedAt.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { HorizontalDivider() }

        when (val content = uiState.content) {
            is DocumentContent.Leaflet -> {
                content.pages.forEach { page ->
                    page.blocks?.forEach { container ->
                        item {
                            LeafletBlockContent(
                                block = container.block,
                                authorDid = content.authorDid,
                            )
                        }
                    }
                }
            }

            is DocumentContent.PlainText -> {
                items(splitIntoParagraphs(content.text)) { paragraph ->
                    PlainTextParagraph(paragraph)
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
            Text(
                "Comments and interactions aren't available in this build yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/**
 * Minimal markdown-ish rendering for content this client doesn't have a
 * bespoke block model for (Markpub's raw markdown, and the plaintext
 * extracted from pckt/Offprint block trees). Recognises blank-line
 * paragraph breaks and `#`-style headings; everything else renders as a
 * plain paragraph. Inline emphasis/links are not parsed — a heavier
 * markdown renderer was avoided since none is already a project dependency
 * (see gradle/libs.versions.toml).
 */
private fun splitIntoParagraphs(text: String): List<String> =
    text.split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }

@Composable
private fun PlainTextParagraph(paragraph: String) {
    val headingLevel = paragraph.takeWhile { it == '#' }.length.coerceAtMost(6)
    if (headingLevel in 1..6 && paragraph.getOrNull(headingLevel) == ' ') {
        val text = paragraph.drop(headingLevel + 1).trim()
        val style = when (headingLevel) {
            1 -> MaterialTheme.typography.headlineSmall
            2 -> MaterialTheme.typography.titleLarge
            else -> MaterialTheme.typography.titleMedium
        }
        Text(text, style = style, modifier = Modifier.fillMaxWidth())
    } else {
        Text(
            paragraph,
            style = MaterialTheme.typography.bodyLarge,
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
