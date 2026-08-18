package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

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
                        if (isVerified == true) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.Verified,
                                contentDescription = "Verified source",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
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
