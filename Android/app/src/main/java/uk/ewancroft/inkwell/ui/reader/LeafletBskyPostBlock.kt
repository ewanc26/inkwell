package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.shared.text.NumberFormat

@Composable
fun BskyPostBlock(block: LeafletBlock) {
    val subject = block.subject ?: return
    val uri = subject.uri

    var post by remember { mutableStateOf<uk.ewancroft.inkwell.data.model.bluesky.BSkyPostView?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(uri) {
        val result = uk.ewancroft.inkwell.data.remote.BSkyPostFetcher.fetchPost(uri)
        if (result != null) {
            post = result
        } else {
            loadError = true
        }
        isLoading = false
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        when {
            isLoading -> {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading post...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            loadError -> {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Bluesky post unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            post != null -> {
                BSkyPostContent(post = post!!)
            }
        }
    }
}

@Composable
private fun BSkyPostContent(post: uk.ewancroft.inkwell.data.model.bluesky.BSkyPostView) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Author row
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AsyncImage(
                model = post.author.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
            )
            Column {
                Text(
                    post.author.displayName ?: post.author.handle ?: "Unknown",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (post.author.handle != null) {
                    Text(
                        "@${post.author.handle}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // Post text
        if (!post.record.text.isNullOrBlank()) {
            Text(
                post.record.text,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2f,
            )
        }

        // Image embed
        val embed = post.embed
        if (embed is uk.ewancroft.inkwell.data.model.bluesky.BSkyEmbed.Images) {
            val first = embed.images.firstOrNull()
            if (first?.thumb != null) {
                AsyncImage(
                    model = first.thumb,
                    contentDescription = first.alt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
            }
        }

        // External link embed
        if (embed is uk.ewancroft.inkwell.data.model.bluesky.BSkyEmbed.External) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            ) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (embed.external.title != null) {
                            Text(embed.external.title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                        }
                        if (embed.external.description != null) {
                            Text(
                                embed.external.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 2,
                            )
                        }
                        if (embed.external.uri != null) {
                            val host = try { java.net.URI(embed.external.uri).host ?: embed.external.uri } catch (_: Exception) { embed.external.uri }
                            Text(host, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                    if (embed.external.thumb != null) {
                        AsyncImage(
                            model = embed.external.thumb,
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

        // Record embed (quoted post)
        if (embed is uk.ewancroft.inkwell.data.model.bluesky.BSkyEmbed.Record) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            embed.record.author?.displayName ?: "Quoted post",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    if (embed.record.value?.text != null) {
                        Text(
                            embed.record.value.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 3,
                        )
                    }
                }
            }
        }

        // Stats row
        Row(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PostStat(icon = Icons.Outlined.ChatBubbleOutline, count = post.replyCount)
            PostStat(icon = Icons.Outlined.Repeat, count = post.repostCount)
            PostStat(icon = Icons.Outlined.FavoriteBorder, count = post.likeCount)
            Spacer(Modifier.weight(1f))
            Text("Bluesky", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun PostStat(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp))
        if (count != null) {
            Text(NumberFormat.formatCount(count), style = MaterialTheme.typography.labelSmall)
        }
    }
}
