package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedContent(
    posts: List<PostItem>,
    isLoading: Boolean,
    feedType: String,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onPostClick: (Int, PostItem) -> Unit = { _, _ -> },
    onViewProfile: (PostItem) -> Unit = {},
    onReportPost: (PostItem) -> Unit = {},
    onReportAccount: (PostItem) -> Unit = {},
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        // Only show the pull-to-refresh spinner for a refresh of an
        // already-populated list — the centered CircularProgressIndicator
        // below owns the empty-list first-load case. Tying this to
        // isLoading unconditionally showed both spinners at once on
        // initial load.
        isRefreshing = isLoading && posts.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
    if (isLoading && posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (posts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Book,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (feedType == "following") "Nothing to read yet" else "No published posts",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (feedType == "following") "Subscribe to publications to see their posts here."
                    else "Posts you publish will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(posts, key = { it.uri }) { post ->
                val index = posts.indexOf(post)
                PostCard(
                    title = post.title,
                    description = post.description,
                    publicationName = post.publicationName,
                    date = post.date,
                    coverUrl = post.coverUrl,
                    authorDisplayName = post.authorDisplayName,
                    authorAvatar = post.authorAvatar,
                    isVerified = post.isVerified,
                    publicationTheme = post.publicationTheme,
                    publicationBasicTheme = post.publicationBasicTheme,
                    isCached = post.isCached,
                    onClick = { onPostClick(index, post) },
                    onViewProfile = if (post.authorDid.isNotBlank()) {
                        { onViewProfile(post) }
                    } else {
                        null
                    },
                    onReportPost = { onReportPost(post) },
                    onReportAccount = if (post.authorDid.isNotBlank()) {
                        { onReportAccount(post) }
                    } else {
                        null
                    },
                )
            }
            if (hasMore) {
                item(key = "sentinel") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        if (isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    LaunchedEffect(Unit) { onLoadMore() }
                }
            }
        }
    }
    }
}
