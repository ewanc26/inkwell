package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.bluesky.BSkyColorRGB
import uk.ewancroft.inkwell.data.model.bluesky.BSkyExternal
import uk.ewancroft.inkwell.data.model.bluesky.BSkyStrongRef
import uk.ewancroft.inkwell.data.model.bluesky.contentWarningLabels
import uk.ewancroft.inkwell.data.model.bluesky.isStandardSiteEnriched
import uk.ewancroft.inkwell.data.model.bluesky.readingTimeLabel
import uk.ewancroft.inkwell.data.model.bluesky.sourceDisplayTitle
import uk.ewancroft.inkwell.util.formatPublishedDate

/**
 * The `external` embed card: a plain link-preview card for ordinary
 * Bluesky links, or an enriched Standard.site card (source identity,
 * reading time, publish/update date, content-warning labels, and tappable
 * chips into any associated Inkwell documents) when the AppView returns
 * Standard.site metadata. Plain cards render exactly as before this change.
 */
@Composable
fun BSkyExternalCard(
    external: BSkyExternal,
    onNavigateToDocument: (String) -> Unit = {},
) {
    if (external.isStandardSiteEnriched()) {
        StandardSiteExternalCard(external, onNavigateToDocument)
    } else {
        PlainExternalCard(external)
    }
}

/** The original plain-link card, unchanged from before Standard.site enrichment. */
@Composable
private fun PlainExternalCard(external: BSkyExternal) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (external.title != null) {
                    Text(external.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                }
                if (external.description != null) {
                    Text(
                        external.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }
                if (external.uri != null) {
                    Text(hostOf(external.uri), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
            if (external.thumb != null) {
                AsyncImage(
                    model = external.thumb,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.small),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/**
 * A Standard.site-enriched external card, styled to read as "definitely
 * one of ours" the way [StandardSitePostBlock]'s card does, while text
 * stays in the default on-surface color so an arbitrary publication theme
 * accent never has to carry contrast on its own — it's only used for the
 * card border and chip accents.
 */
@Composable
private fun StandardSiteExternalCard(external: BSkyExternal, onNavigateToDocument: (String) -> Unit) {
    val themeAccent = external.source?.theme?.accentRGB?.toComposeColorOrNull() ?: MaterialTheme.colorScheme.primary
    val bodyColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.35f)),
    ) {
        Column {
            if (external.thumb != null) {
                AsyncImage(
                    model = external.thumb,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val sourceTitle = external.sourceDisplayTitle()
                if (sourceTitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (external.source?.icon != null) {
                            AsyncImage(
                                model = external.source.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        Text(
                            sourceTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = bodyColor.copy(alpha = 0.6f),
                            maxLines = 1,
                        )
                    }
                }

                if (external.title != null) {
                    Text(external.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                }
                if (external.description != null) {
                    Text(
                        external.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = bodyColor.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }

                val readingLabel = external.readingTimeLabel()
                val dateLabel = dateLabelFor(external)
                if (readingLabel != null || dateLabel != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (readingLabel != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = bodyColor.copy(alpha = 0.5f))
                                Text(readingLabel, style = MaterialTheme.typography.labelSmall, color = bodyColor.copy(alpha = 0.5f))
                            }
                        }
                        if (dateLabel != null) {
                            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = bodyColor.copy(alpha = 0.5f))
                        }
                    }
                }

                val warnings = external.contentWarningLabels()
                if (warnings.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.error)
                        Text(
                            warnings.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                val refs = external.associatedRefs.orEmpty().filter { it.uri != null }
                if (refs.isNotEmpty()) {
                    AssociatedRefsRow(refs, themeAccent, onNavigateToDocument)
                }
            }
        }
    }
}

/**
 * Tappable chips for `associatedRefs`, routed through the caller's
 * `onNavigateToDocument` — which threads back to [uk.ewancroft.inkwell.ui.navigation.InkwellNavHost]'s
 * existing `navigateToPost` (`post/{uri}` route) — rather than only
 * falling back to opening the external web URL.
 */
@Composable
private fun AssociatedRefsRow(refs: List<BSkyStrongRef>, accent: Color, onNavigateToDocument: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(refs) { ref ->
            val uri = ref.uri ?: return@items
            AssistChip(
                onClick = { onNavigateToDocument(uri) },
                label = { Text("Related") },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null, modifier = Modifier.size(14.dp))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = accent.copy(alpha = 0.12f),
                    labelColor = accent,
                    leadingIconContentColor = accent,
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Related Standard.site content. Opens the associated document in Inkwell."
                },
            )
        }
    }
}

private fun dateLabelFor(external: BSkyExternal): String? {
    val raw = external.updatedAt ?: external.createdAt ?: return null
    val prefix = if (external.updatedAt != null) "Updated" else "Published"
    return "$prefix ${raw.formatPublishedDate()}"
}

private fun hostOf(uri: String): String =
    try { java.net.URI(uri).host ?: uri } catch (_: Exception) { uri }

private fun BSkyColorRGB?.toComposeColorOrNull(): Color? {
    val r = this?.r ?: return null
    val g = this.g ?: return null
    val b = this.b ?: return null
    if (r !in 0..255 || g !in 0..255 || b !in 0..255) return null
    return Color(r / 255f, g / 255f, b / 255f)
}
