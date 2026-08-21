package uk.ewancroft.inkwell.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material3.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.remote.BSkyListFetcher
import uk.ewancroft.inkwell.shared.content.SearchBackendUrl
import uk.ewancroft.inkwell.shared.support.SupportersList
import uk.ewancroft.inkwell.ui.feedback.FeedbackDialog

@Composable
fun CreditsView(
    appVersion: String,
    onSignOut: () -> Unit = {},
    onDismiss: () -> Unit,
    isAuthenticated: Boolean = true,
) {
    val context = LocalContext.current
    var showFeedback by remember { mutableStateOf(false) }
    var showSupport by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }
    var supporters by remember { mutableStateOf<List<BlueskyProfile>>(emptyList()) }

    if (showFeedback) {
        FeedbackDialog(onDismiss = { showFeedback = false })
    }

    if (showSupport) {
        SupportDialog(onDismiss = { showSupport = false })
    }

    if (showPrivacy) {
        LegalDocumentDialog(documentType = LegalDocumentType.PrivacyPolicy, onDismiss = { showPrivacy = false })
    }

    if (showTerms) {
        LegalDocumentDialog(documentType = LegalDocumentType.TermsOfService, onDismiss = { showTerms = false })
    }

    LaunchedEffect(Unit) {
        supporters = BSkyListFetcher.fetchListMembers(SupportersList.URI)
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                InkwellMark(
                    modifier = Modifier.height(56.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                Text("Inkwell", style = MaterialTheme.typography.titleLarge)
                Text(
                    appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // About
                SectionHeader("About")
                Text(
                    "A native reader and writer for the Standard.site publishing ecosystem on AT Protocol — read, discover, and publish portable writing from your own PDS.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Built On
                SectionHeader("Built On")
                CreditRow(title = "atproto-kotlin", detail = "AT Protocol SDK + OAuth 2.1 by kikin81", url = "https://github.com/kikin81/atproto-kotlin", openUrl = ::openUrl)
                CreditRow(title = "Standard.site", detail = "The publishing protocol Inkwell reads and writes", url = "https://standard.site", openUrl = ::openUrl)
                CreditRow(title = "pub search", detail = "Cross-platform Standard.site search index", url = SearchBackendUrl.BASE, openUrl = ::openUrl)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Inkwell reads and writes Leaflet, Markpub, pckt, and Offprint content alongside the shared site.standard.* records.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (supporters.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // Supporters
                    SectionHeader("Supporters")
                    supporters.forEach { supporter ->
                        SupporterRow(supporter = supporter, openUrl = ::openUrl)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Everyone who's tipped Inkwell via Ko-fi or GitHub Sponsors, listed on Bluesky. Thank you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Support
                SectionHeader("Support")
                SupportRow(onClick = { showSupport = true })
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showFeedback = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Feedback,
                        contentDescription = null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Send Feedback", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Bugs, questions, anything",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CreditRow(title = "Source on GitHub", detail = "ewanc26/inkwell", url = "https://github.com/ewanc26/inkwell", openUrl = ::openUrl)
                CreditRow(title = "Ewan Croft", detail = "Developer", url = "https://ewancroft.uk", openUrl = ::openUrl)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Legal
                SectionHeader("Legal")
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickable { showPrivacy = true },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Terms of Service",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().clickable { showTerms = true },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "AGPL 3.0 License",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (isAuthenticated) {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // Sign Out
                    TextButton(
                        onClick = {
                            onDismiss()
                            onSignOut()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "Inkwell for iOS is also available on GitHub.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CreditRow(title: String, detail: String, url: String, openUrl: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { openUrl(url) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SupporterRow(supporter: BlueskyProfile, openUrl: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { openUrl("https://bsky.app/profile/${supporter.handle}") }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (supporter.avatar != null) {
            AsyncImage(
                model = supporter.avatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                supporter.displayName?.ifEmpty { null } ?: "@${supporter.handle}",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                "@${supporter.handle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

/**
 * Opens [SupportDialog] — the tip jar and non-monetary support options,
 * which iOS reaches the same way via a `SupportView` navigation link
 * rather than listing Ko-fi and Sponsors inline here.
 */
@Composable
private fun SupportRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Favorite,
            contentDescription = null,
            Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Support Inkwell", style = MaterialTheme.typography.bodyMedium)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
