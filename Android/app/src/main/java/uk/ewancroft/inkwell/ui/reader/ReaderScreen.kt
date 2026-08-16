package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.ui.components.CreditsView
import uk.ewancroft.inkwell.ui.components.InkwellMark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Following", "Yours")

    var showCredits by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember {
        try {
            val pkg = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${pkg.versionName} (${pkg.longVersionCode})"
        } catch (_: Exception) { "Version 1.3.0 (5)" }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InkwellMark(
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text("Reader", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCredits = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = { Text(title) },
                    )
                }
            }

            when (uiState.selectedTab) {
                0 -> FeedContent(
                    posts = uiState.followingPosts,
                    isLoading = uiState.isLoadingFollowing,
                    feedType = "following",
                    onRefresh = { viewModel.loadData() },
                    isLoadingMore = uiState.isLoadingMoreFollowing,
                    hasMore = uiState.hasMoreFollowing,
                    onLoadMore = { viewModel.loadNextFollowingPage() },
                    onPostClick = { index, post ->
                        val prev = if (index > 0) uiState.followingPosts[index - 1] else null
                        val next = if (index < uiState.followingPosts.lastIndex) uiState.followingPosts[index + 1] else null
                        onNavigateToPost(
                            post.uri,
                            prev?.uri, prev?.title,
                            next?.uri, next?.title
                        )
                    },
                )
                1 -> FeedContent(
                    posts = uiState.yoursPosts,
                    isLoading = uiState.isLoadingYours,
                    feedType = "yours",
                    onRefresh = { viewModel.loadData() },
                    onPostClick = { index, post ->
                        val prev = if (index > 0) uiState.yoursPosts[index - 1] else null
                        val next = if (index < uiState.yoursPosts.lastIndex) uiState.yoursPosts[index + 1] else null
                        onNavigateToPost(
                            post.uri,
                            prev?.uri, prev?.title,
                            next?.uri, next?.title
                        )
                    },
                )
            }
        }
    }

    if (uiState.error != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.loadData() }) {
                    Text("Retry")
                }
            }
        ) {
            Text(uiState.error!!)
        }
    }

    if (showCredits) {
        CreditsView(
            appVersion = appVersion,
            onSignOut = onSignOut,
            onDismiss = { showCredits = false },
        )
    }
}

@Composable
private fun FeedContent(
    posts: List<PostItem>,
    isLoading: Boolean,
    feedType: String,
    onRefresh: () -> Unit,
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onPostClick: (Int, PostItem) -> Unit = { _, _ -> },
) {
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = isLoading,
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
                    onClick = { onPostClick(index, post) },
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

@Composable
fun PostCard(
    title: String,
    description: String?,
    publicationName: String?,
    date: String,
    coverUrl: String?,
    authorDisplayName: String? = null,
    authorAvatar: String? = null,
    onClick: () -> Unit = {},
) {
    val cardBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val cardContainerColor = MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .drawBehind {
                drawRect(
                    color = cardBorderColor,
                    topLeft = Offset(0.5f, 0.5f),
                    size = Size(size.width - 1f, size.height - 1f),
                    style = Stroke(width = 1f),
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            if (coverUrl != null) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (authorAvatar != null) {
                        AsyncImage(
                            model = authorAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (authorDisplayName != null) {
                        Text(
                            authorDisplayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (publicationName != null) {
                        Text(
                            "·",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            publicationName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
