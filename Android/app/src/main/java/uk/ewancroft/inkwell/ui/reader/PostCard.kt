package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.ui.theme.LocalForceDarkTheme
import uk.ewancroft.inkwell.util.AccessibilityPreferences
import uk.ewancroft.inkwell.util.CustomisationPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun PostCard(
    title: String,
    description: String?,
    publicationName: String?,
    date: String,
    coverUrl: String?,
    authorDisplayName: String? = null,
    authorAvatar: String? = null,
    isVerified: Boolean? = null,
    publicationTheme: PublicationTheme? = null,
    publicationBasicTheme: BasicTheme? = null,
    onClick: () -> Unit = {},
    onViewProfile: (() -> Unit)? = null,
    onReportPost: (() -> Unit)? = null,
    onReportAccount: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val isDarkTheme = LocalForceDarkTheme.current ?: isSystemInDarkTheme()
    val publicationThemeIsPresent = publicationTheme != null || publicationBasicTheme != null
    val readerTheme = remember(publicationTheme, publicationBasicTheme, isDarkTheme) {
        ReaderTheme.resolve(
            publicationTheme = publicationTheme,
            basicTheme = publicationBasicTheme,
            isDarkTheme = isDarkTheme,
            overrideAccentRgb = CustomisationPreferences.getAccentColorRgbInt(context),
            overrideFontFamily = CustomisationPreferences.getFontFamilyOverride(context),
            increaseContrast = AccessibilityPreferences.getIncreaseContrast(context),
        )
    }
    val cardContainerColor = if (publicationThemeIsPresent) readerTheme.background else MaterialTheme.colorScheme.surface
    val foreground = if (publicationThemeIsPresent) readerTheme.foreground else MaterialTheme.colorScheme.onSurface
    val secondaryForeground = if (publicationThemeIsPresent) foreground.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val accent = if (publicationThemeIsPresent) readerTheme.accent else MaterialTheme.colorScheme.primary
    val cardBorderColor = foreground.copy(alpha = 0.1f)
    val accessibilityLabel = buildList {
        add(title)
        authorDisplayName?.takeIf(String::isNotBlank)?.let { add("By $it") }
        description?.takeIf(String::isNotBlank)?.let(::add)
        publicationName?.takeIf(String::isNotBlank)?.let { add("Published in $it") }
        add("Published $date")
    }.joinToString(separator = ". ")
    val hasOverflowActions = onViewProfile != null || onReportPost != null || onReportAccount != null
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                    if (isVerified == true) {
                        stateDescription = "Verified source"
                    }
                }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (hasOverflowActions) 40.dp else 0.dp),
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
                                color = secondaryForeground,
                            )
                        }
                        if (hasOverflowActions) {
                            Spacer(Modifier.weight(1f))
                            Spacer(Modifier.width(56.dp))
                        }
                    }

                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = foreground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (description != null) {
                        Text(
                            description,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryForeground,
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
                            color = secondaryForeground,
                        )
                        if (publicationName != null) {
                            Text(
                                "·",
                                color = secondaryForeground,
                            )
                            Text(
                                publicationName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (isVerified == true) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = accent,
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = secondaryForeground.copy(alpha = 0.55f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        if (hasOverflowActions) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            ) {
                IconButton(
                    onClick = { overflowMenuExpanded = true },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "More actions for $title",
                        tint = secondaryForeground,
                    )
                }
                DropdownMenu(
                    expanded = overflowMenuExpanded,
                    onDismissRequest = { overflowMenuExpanded = false },
                ) {
                    onViewProfile?.let { viewProfile ->
                        DropdownMenuItem(
                            text = { Text("View profile") },
                            onClick = {
                                overflowMenuExpanded = false
                                viewProfile()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null)
                            },
                        )
                    }
                    onReportPost?.let { reportPost ->
                        DropdownMenuItem(
                            text = { Text("Report post") },
                            onClick = {
                                overflowMenuExpanded = false
                                reportPost()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Report, contentDescription = null)
                            },
                        )
                    }
                    onReportAccount?.let { reportAccount ->
                        DropdownMenuItem(
                            text = { Text("Report account") },
                            onClick = {
                                overflowMenuExpanded = false
                                reportAccount()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Report, contentDescription = null)
                            },
                        )
                    }
                }
            }
        }
    }
}
