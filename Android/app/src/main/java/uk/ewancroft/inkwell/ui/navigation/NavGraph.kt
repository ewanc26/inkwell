package uk.ewancroft.inkwell.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.ewancroft.inkwell.ui.auth.LoginScreen
import uk.ewancroft.inkwell.ui.reader.ReaderScreen
import uk.ewancroft.inkwell.ui.reader.PostDetailScreen
import uk.ewancroft.inkwell.ui.reader.InkwellNotificationViewModel
import uk.ewancroft.inkwell.ui.writer.WriterScreen
import uk.ewancroft.inkwell.ui.discover.DiscoverScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Reader   : Screen("reader",   "Read",     Icons.Outlined.Book,    Icons.Filled.Book)
    data object Discover : Screen("discover", "Discover", Icons.Outlined.Explore,  Icons.Filled.Explore)
    data object Writer   : Screen("writer",   "Write",    Icons.Outlined.Edit,     Icons.Filled.Edit)
}

val bottomNavItems = listOf(Screen.Reader, Screen.Discover, Screen.Writer)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkwellNavHost(
    isAuthenticated: Boolean,
    onSignOut: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    pendingDocumentUri: String? = null,
    onDocumentNavigated: () -> Unit = {},
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = isAuthenticated && currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    val notificationViewModel: InkwellNotificationViewModel = hiltViewModel()
    val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    // The worker updates the persisted count out-of-band, so re-read it
    // whenever the current destination changes (e.g. landing back on the
    // Reader tab) rather than only once at first composition.
    LaunchedEffect(currentDestination?.route) {
        notificationViewModel.refreshUnreadCount()
    }

    LaunchedEffect(pendingDocumentUri) {
        if (pendingDocumentUri != null) {
            val encoded = URLEncoder.encode(pendingDocumentUri, StandardCharsets.UTF_8.name())
            navController.navigate("post/$encoded") {
                launchSingleTop = true
            }
            onDocumentNavigated()
        }
    }

    fun navigateToPost(uri: String, prevUri: String?, prevTitle: String?, nextUri: String?, nextTitle: String?) {
        val encoded = URLEncoder.encode(uri, StandardCharsets.UTF_8.name())
        val prev = prevUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) } ?: ""
        val next = nextUri?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) } ?: ""
        val prevT = prevTitle?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) } ?: ""
        val nextT = nextTitle?.let { URLEncoder.encode(it, StandardCharsets.UTF_8.name()) } ?: ""
        navController.navigate("post/$encoded?prev=$prev&next=$next&prevTitle=$prevT&nextTitle=$nextT") {
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                val icon = @Composable {
                                    Icon(
                                        if (selected) screen.selectedIcon else screen.icon,
                                        contentDescription = screen.label,
                                    )
                                }
                                // Mirrors iOS's `Tab("Read", ...).badge(notificationManager.unreadCount)`.
                                if (screen == Screen.Reader && unreadCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$unreadCount") } }) { icon() }
                                } else {
                                    icon()
                                }
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) Screen.Reader.route else "login",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("login") {
                LoginScreen()
            }

            composable(Screen.Reader.route) {
                ReaderScreen(
                    onNavigateToPost = { uri, prevUri, prevTitle, nextUri, nextTitle ->
                        navigateToPost(uri, prevUri, prevTitle, nextUri, nextTitle)
                    },
                    onSignOut = onSignOut,
                )
            }

            composable(Screen.Discover.route) {
                DiscoverScreen(
                    onNavigateToPost = { uri, prevUri, prevTitle, nextUri, nextTitle ->
                        navigateToPost(uri, prevUri, prevTitle, nextUri, nextTitle)
                    },
                    onSignOut = onSignOut,
                )
            }

            composable(Screen.Writer.route) {
                WriterScreen(
                    onSignOut = onSignOut,
                    onNavigateToPost = { uri, prevUri, prevTitle, nextUri, nextTitle ->
                        navigateToPost(uri, prevUri, prevTitle, nextUri, nextTitle)
                    },
                )
            }

            composable(
                route = "post/{uri}?prev={prev}&next={next}&prevTitle={prevTitle}&nextTitle={nextTitle}",
                arguments = listOf(
                    androidx.navigation.navArgument("prev") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    androidx.navigation.navArgument("next") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    androidx.navigation.navArgument("prevTitle") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                    androidx.navigation.navArgument("nextTitle") { type = androidx.navigation.NavType.StringType; defaultValue = "" },
                )
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("uri") ?: return@composable
                val uri = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name())
                val prev = backStackEntry.arguments?.getString("prev")?.takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val next = backStackEntry.arguments?.getString("next")?.takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val prevTitle = backStackEntry.arguments?.getString("prevTitle")?.takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                val nextTitle = backStackEntry.arguments?.getString("nextTitle")?.takeIf { it.isNotBlank() }?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                PostDetailScreen(
                    uri = uri,
                    previousUri = prev,
                    previousTitle = prevTitle,
                    nextUri = next,
                    nextTitle = nextTitle,
                    onBack = { navController.popBackStack() },
                    onNavigateToPost = { target, pUri, pTitle, nUri, nTitle ->
                        navigateToPost(target, pUri, pTitle, nUri, nTitle)
                    },
                )
            }
        }
    }
}
