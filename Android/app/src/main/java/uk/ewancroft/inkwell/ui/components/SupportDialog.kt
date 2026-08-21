package uk.ewancroft.inkwell.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Tip jar + alternate support methods, opened from [CreditsView] and
 * mirroring iOS `SupportView.swift` row for row. Inkwell is free on both
 * platforms — these are external links (Ko-fi, GitHub Sponsors), not
 * in-app purchases, since a self-hosted F-Droid repo has no store
 * billing to hook Play Billing into.
 */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun shareInkwell() {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://inkwell.ewancroft.uk")
        }
        context.startActivity(Intent.createChooser(share, null))
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = true)) {
        Surface(shape = MaterialTheme.shapes.large, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            ) {
                Text("Support Inkwell", style = MaterialTheme.typography.titleLarge)

                Spacer(Modifier.height(16.dp))
                SupportMethodRow(
                    icon = Icons.Filled.LocalCafe,
                    title = "Ko-fi",
                    detail = "Buy me a tea — £2.99 suggested",
                    onClick = { openUrl("https://ko-fi.com/ewancroft?amount=2.99") },
                )
                SupportMethodRow(
                    icon = Icons.Filled.Favorite,
                    title = "GitHub Sponsors",
                    detail = "Sponsor development work",
                    onClick = { openUrl("https://github.com/sponsors/ewanc26") },
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    "Non-Monetary",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                SupportMethodRow(
                    icon = Icons.Filled.Share,
                    title = "Share Inkwell",
                    detail = "Word of mouth is the best support",
                    onClick = ::shareInkwell,
                )
                SupportMethodRow(
                    icon = Icons.Filled.Code,
                    title = "Contribute",
                    detail = "Bugs, features, pull requests",
                    onClick = { openUrl("https://github.com/ewanc26/inkwell") },
                )

                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun SupportMethodRow(
    icon: ImageVector,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
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
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
