package uk.ewancroft.inkwell.ui.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import uk.ewancroft.inkwell.R
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.model.bluesky.isVerifiedAccount
import uk.ewancroft.inkwell.ui.moderation.ReportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    did: String,
    onBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showReportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(did) {
        viewModel.loadProfile(did)
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
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.reader_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        when {
            uiState.isLoading && uiState.profile == null -> LoadingProfile(Modifier.padding(padding))
            uiState.profile != null -> ProfileContent(
                profile = uiState.profile!!,
                onReport = { showReportDialog = true },
                modifier = Modifier.padding(padding),
            )
            uiState.error != null -> ProfileError(
                message = uiState.error!!,
                onRetry = { viewModel.loadProfile(did) },
                modifier = Modifier.padding(padding),
            )
        }
    }

    if (showReportDialog) {
        ReportDialog(
            subject = did,
            onDismiss = { showReportDialog = false },
            onSubmit = { reasonType, reason ->
                showReportDialog = false
                viewModel.submitReport(did, reasonType, reason)
            },
        )
    }

}

@Composable
private fun LoadingProfile(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ProfileError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text(stringResource(R.string.profile_unavailable), style = MaterialTheme.typography.titleMedium)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.profile_retry)) }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: BlueskyProfile,
    onReport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    fun openExternal(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            ProfileHeader(profile = profile)
        }
        item {
            HorizontalDivider()
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp),
            ) {
                profile.description?.trim()?.takeIf(String::isNotEmpty)?.let { description ->
                    Text(stringResource(R.string.reader_about), style = MaterialTheme.typography.titleMedium)
                    Text(description, style = MaterialTheme.typography.bodyLarge)
                }

                profile.links?.filter { isOpenableHttpUrl(it.url) }?.takeIf { it.isNotEmpty() }?.let { links ->
                    Text(stringResource(R.string.profile_links), style = MaterialTheme.typography.titleMedium)
                    links.forEach { link ->
                        ProfileLinkRow(
                            url = link.url!!,
                            title = link.title,
                            icon = link.icon,
                            onOpen = ::openExternal,
                        )
                    }
                }

                Text(stringResource(R.string.profile_account), style = MaterialTheme.typography.titleMedium)
                Text(
                    profile.did,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://bsky.app/profile/${profile.handle}"),
                        )
                        context.startActivity(intent)
                    },
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.profile_open_bluesky))
                }

                OutlinedButton(
                    onClick = onReport,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Report, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reader_report_account))
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(profile: BlueskyProfile) {
    val displayName = profile.displayName?.trim()?.takeIf(String::isNotEmpty) ?: profile.handle
    val pronouns = profile.pronouns?.trim()?.takeIf(String::isNotEmpty)
    val accessibilityLabel = buildList {
        add(displayName)
        add("@${profile.handle}")
        pronouns?.let { add(it) }
        if (profile.isVerifiedAccount()) { add(stringResource(R.string.profile_verified_account)) }
        profile.followersCount?.let { add(pluralStringResource(R.plurals.profile_followers_count, it, it)) }
        profile.followsCount?.let { add(pluralStringResource(R.plurals.profile_following_count, it, it)) }
        profile.postsCount?.let { add(pluralStringResource(R.plurals.profile_posts_count, it, it)) }
    }.joinToString(separator = ". ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            },
    ) {
        profile.banner?.takeIf(String::isNotBlank)?.let { banner ->
            AsyncImage(
                model = banner,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(20.dp),
        ) {
            if (profile.avatar != null) {
                AsyncImage(
                    model = profile.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (profile.isVerifiedAccount()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    "@${profile.handle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                pronouns?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            ProfileStatistic(profile.followersCount, R.plurals.profile_followers_count)
            ProfileStatistic(profile.followsCount, R.plurals.profile_following_count)
            ProfileStatistic(profile.postsCount, R.plurals.profile_posts_count)
        }
    }
}

private fun isOpenableHttpUrl(url: String?): Boolean {
    val scheme = url?.let { Uri.parse(it).scheme }?.lowercase()
    return scheme == "http" || scheme == "https"
}

@Composable
private fun ProfileLinkRow(
    url: String,
    title: String?,
    icon: String?,
    onOpen: (String) -> Unit,
) {
    val label = title?.trim()?.takeIf(String::isNotEmpty) ?: Uri.parse(url).host ?: url
    TextButton(onClick = { onOpen(url) }) {
        if (icon != null) {
            AsyncImage(
                model = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
        }
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ProfileStatistic(value: Int?, label: Int) {
    val formattedLabel = value?.let { pluralStringResource(label, it, it) } ?: stringResource(R.string.profile_count_unavailable)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.width(8.dp))
        Text(formattedLabel, style = MaterialTheme.typography.bodyMedium)
    }
}
