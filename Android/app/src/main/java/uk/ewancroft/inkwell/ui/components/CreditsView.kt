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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.bluesky.BlueskyProfile
import uk.ewancroft.inkwell.data.remote.BSkyListFetcher
import uk.ewancroft.inkwell.shared.content.SearchBackendUrl
import uk.ewancroft.inkwell.shared.support.SupportersList
import uk.ewancroft.inkwell.ui.feedback.FeedbackDialog
import uk.ewancroft.inkwell.R

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

    if (isAuthenticated && showFeedback) {
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
                SectionHeader(stringResource(R.string.credits_about))
                Text(
                    stringResource(R.string.credits_about_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Built On
                SectionHeader(stringResource(R.string.credits_built_on))
                CreditRow(title = "atproto-kotlin", detail = stringResource(R.string.credits_atproto_detail), url = "https://github.com/kikin81/atproto-kotlin", openUrl = ::openUrl)
                CreditRow(title = "Standard.site", detail = stringResource(R.string.credits_standard_detail), url = "https://standard.site", openUrl = ::openUrl)
                CreditRow(title = "pub search", detail = stringResource(R.string.credits_search_detail), url = SearchBackendUrl.BASE, openUrl = ::openUrl)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.credits_formats_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (supporters.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // Supporters
                    SectionHeader(stringResource(R.string.credits_supporters))
                    supporters.forEach { supporter ->
                        SupporterRow(supporter = supporter, openUrl = ::openUrl)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.credits_supporters_summary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Support
                SectionHeader(stringResource(R.string.credits_support))
                SupportRow(onClick = { showSupport = true })
                if (isAuthenticated) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .accessibleAction(
                                description = stringResource(R.string.credits_send_feedback) + ". " + stringResource(R.string.credits_feedback_summary),
                                actionLabel = stringResource(R.string.credits_open_feedback),
                                onClick = { showFeedback = true },
                            )
                            .padding(vertical = 6.dp),
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
                            Text(stringResource(R.string.credits_send_feedback), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                stringResource(R.string.credits_feedback_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                CreditRow(title = stringResource(R.string.credits_source_github), detail = "ewanc26/inkwell", url = "https://github.com/ewanc26/inkwell", openUrl = ::openUrl)
                CreditRow(title = "Ewan Croft", detail = stringResource(R.string.credits_developer), url = "https://ewancroft.uk", openUrl = ::openUrl)

                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Legal
                SectionHeader(stringResource(R.string.credits_legal))
                Text(
                    stringResource(R.string.credits_privacy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .accessibleAction(
                            description = stringResource(R.string.credits_privacy),
                            actionLabel = stringResource(R.string.credits_open_privacy),
                            onClick = { showPrivacy = true },
                        ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.credits_terms),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .accessibleAction(
                            description = stringResource(R.string.credits_terms),
                            actionLabel = stringResource(R.string.credits_open_terms),
                            onClick = { showTerms = true },
                        ),
                )
                Spacer(Modifier.height(8.dp))
                CreditRow(
                    title = stringResource(R.string.credits_license),
                    detail = stringResource(R.string.credits_license_detail),
                    url = "https://github.com/ewanc26/inkwell/blob/main/APP_STORE_EXCEPTION.md",
                    openUrl = ::openUrl,
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
                        Text(stringResource(R.string.credits_sign_out))
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.credits_ios_github),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private fun Modifier.accessibleAction(
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
): Modifier = this
    .defaultMinSize(minHeight = 48.dp)
    .semantics(mergeDescendants = true) {
        contentDescription = description
    }
    .clickable(
        role = Role.Button,
        onClickLabel = actionLabel,
        onClick = onClick,
    )

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
        modifier = Modifier
            .fillMaxWidth()
            .accessibleAction(
                description = "$title. $detail",
                actionLabel = stringResource(R.string.credits_open_item, title),
                onClick = { openUrl(url) },
            )
            .padding(vertical = 6.dp),
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
    val displayName = supporter.displayName?.takeIf(String::isNotBlank)
    val accessibilityDescription = if (displayName != null) {
        "$displayName. @${supporter.handle}"
    } else {
        "@${supporter.handle}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .accessibleAction(
                description = accessibilityDescription,
                actionLabel = stringResource(R.string.credits_open_supporter),
                onClick = { openUrl("https://bsky.app/profile/${supporter.handle}") },
            )
            .padding(vertical = 6.dp),
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
                displayName ?: "@${supporter.handle}",
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
        modifier = Modifier
            .fillMaxWidth()
            .accessibleAction(
                description = stringResource(R.string.credits_support_inkwell),
                actionLabel = stringResource(R.string.credits_open_support),
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
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
            Text(stringResource(R.string.credits_support_inkwell), style = MaterialTheme.typography.bodyMedium)
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
