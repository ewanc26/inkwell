package uk.ewancroft.inkwell.ui.reader

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Logout
import uk.ewancroft.inkwell.ui.components.InkwellMark
import uk.ewancroft.inkwell.ui.components.NotificationsDialog
import uk.ewancroft.inkwell.ui.components.SettingsDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.ewancroft.inkwell.TestingConfig
import uk.ewancroft.inkwell.ui.reader.InkwellNotificationViewModel
import uk.ewancroft.inkwell.ui.components.CreditsView
import uk.ewancroft.inkwell.ui.moderation.ReportDialog
import uk.ewancroft.inkwell.ui.offline.OfflineStatusBanner
import uk.ewancroft.inkwell.ui.offline.rememberNetworkAvailable
import uk.ewancroft.inkwell.util.TipPromptManager

private data class ReaderReportTarget(
    val subject: String,
    val recordCid: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel = hiltViewModel(),
    notificationViewModel: InkwellNotificationViewModel = hiltViewModel(),
    userLexiconViewModel: UserLexiconViewModel = hiltViewModel(),
    onNavigateToPost: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onNavigateToProfile: (String) -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tabs = listOf("Following", "Yours")

    var showCredits by remember { mutableStateOf(false) }
    var showTipPrompt by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReaderReportTarget?>(null) }
    val notifications by notificationViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    val notificationsEnabled by notificationViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val userLexiconEnabled by userLexiconViewModel.isInkwellUser.collectAsStateWithLifecycle()
    val userLexiconBusy by userLexiconViewModel.isBusy.collectAsStateWithLifecycle()
    val isNetworkAvailable = rememberNetworkAvailable()
    val snackbarHostState = remember { SnackbarHostState() }

    val appContext = androidx.compose.ui.platform.LocalContext.current
    val appVersion = remember { uk.ewancroft.inkwell.util.appVersionString(appContext) }

    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRequestedNotificationPermission = true
        if (isGranted) {
            notificationViewModel.schedulePeriodicPoll()
        }
    }

    if (!TestingConfig.suppressesInterruptions && !hasRequestedNotificationPermission) {
        LaunchedEffect(Unit) {
            hasRequestedNotificationPermission = true
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        if (!TestingConfig.suppressesInterruptions &&
            uk.ewancroft.inkwell.util.TipPromptManager.shouldShowTip(appContext)
        ) {
            showTipPrompt = true
        }
    }

    // Mirrors iOS BrowseDocumentsView's `.task { ...; notificationManager.markAllAsRead() }`.
    LaunchedEffect(Unit) {
        notificationViewModel.markAllAsRead()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { message ->
            when (snackbarHostState.showSnackbar(message, actionLabel = "Retry")) {
                SnackbarResult.ActionPerformed -> viewModel.loadData()
                SnackbarResult.Dismissed -> viewModel.dismissError()
            }
        }
    }

    LaunchedEffect(uiState.reportError) {
        uiState.reportError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissReportError()
        }
    }

    LaunchedEffect(uiState.reportConfirmation) {
        uiState.reportConfirmation?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissReportConfirmation()
        }
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
                    IconButton(onClick = {
                        notificationViewModel.refreshNotifications()
                        showNotifications = true
                    }) {
                        if (unreadCount > 0) {
                            BadgedBox(badge = { Badge { Text("$unreadCount") } }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                        } else {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = { showCredits = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "About")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.loadData() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            if (!isNetworkAvailable) {
                OfflineStatusBanner()
            }
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
                    onViewProfile = { post -> onNavigateToProfile(post.authorDid) },
                    onReportPost = { post ->
                        reportTarget = ReaderReportTarget(post.uri, post.recordCid)
                    },
                    onReportAccount = { post ->
                        reportTarget = ReaderReportTarget(post.authorDid)
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
                    onViewProfile = { post -> onNavigateToProfile(post.authorDid) },
                    onReportPost = { post ->
                        reportTarget = ReaderReportTarget(post.uri, post.recordCid)
                    },
                    onReportAccount = { post ->
                        reportTarget = ReaderReportTarget(post.authorDid)
                    },
                )
            }
        }
    }

    reportTarget?.let { target ->
        ReportDialog(
            subject = target.subject,
            onDismiss = { reportTarget = null },
            onSubmit = { reasonType, reason ->
                reportTarget = null
                viewModel.submitReport(
                    subject = target.subject,
                    recordCid = target.recordCid,
                    reasonType = reasonType,
                    reason = reason,
                )
            },
        )
    }

    if (showCredits) {
        CreditsView(
            appVersion = appVersion,
            onSignOut = onSignOut,
            onDismiss = { showCredits = false },
        )
    }

    LaunchedEffect(showSettings) {
        if (showSettings) userLexiconViewModel.load()
    }

    if (showNotifications) {
        NotificationsDialog(
            notifications = notifications,
            onOpenDocument = { uri -> onNavigateToPost(uri, null, null, null, null) },
            onClearAll = { notificationViewModel.clearAll() },
            onDismiss = { showNotifications = false },
        )
    }

    if (showSettings) {
        SettingsDialog(
            appVersion = appVersion,
            notificationsEnabled = notificationsEnabled,
            onNotificationsEnabledChange = { notificationViewModel.setNotificationsEnabled(it) },
            userLexiconEnabled = userLexiconEnabled,
            userLexiconBusy = userLexiconBusy,
            onUserLexiconEnabledChange = { userLexiconViewModel.setInkwellUser(it) },
            onSignOut = onSignOut,
            onDismiss = {
                showSettings = false
                viewModel.loadData()
            },
        )
    }

    if (showTipPrompt) {
        AlertDialog(
            onDismissRequest = {
                showTipPrompt = false
                uk.ewancroft.inkwell.util.TipPromptManager.markShown(appContext)
            },
            title = { Text("Enjoying Inkwell?") },
            text = { Text("If you find Inkwell useful, consider buying me a coffee to support ongoing development.") },
            confirmButton = {
                TextButton(onClick = {
                    showTipPrompt = false
                    uk.ewancroft.inkwell.util.TipPromptManager.markShown(appContext)
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/ewancroft"))
                    appContext.startActivity(intent)
                }) {
                    Text("Tip me")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTipPrompt = false
                    uk.ewancroft.inkwell.util.TipPromptManager.markShown(appContext)
                }) {
                    Text("Maybe later")
                }
            },
        )
    }
}
