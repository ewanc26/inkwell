package uk.ewancroft.inkwell.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.ewancroft.inkwell.data.model.common.BlobRef
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.data.model.content.LeafletFacet
import uk.ewancroft.inkwell.data.model.content.LeafletPollDefinition
import uk.ewancroft.inkwell.data.model.content.LeafletPollVote
import uk.ewancroft.inkwell.data.model.content.ListItem as ListItemModel
import uk.ewancroft.inkwell.data.repository.PdsRepository
import uk.ewancroft.inkwell.util.formatPublishedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafletBlockContent(
    block: LeafletBlock,
    authorDid: String,
    modifier: Modifier = Modifier,
    alignment: String? = null,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    val alignModifier = modifier.fillMaxWidth()
    val textAlign = when {
        alignment?.endsWith("textAlignCenter") == true -> TextAlign.Center
        alignment?.endsWith("textAlignRight") == true -> TextAlign.End
        else -> TextAlign.Start
    }

    when (block.type) {
        "pub.leaflet.blocks.text" -> TextBlock(block, alignModifier, textAlign)
        "pub.leaflet.blocks.header" -> HeaderBlock(block, alignModifier, textAlign)
        "pub.leaflet.blocks.paragraph", "pub.leaflet.blocks.blockquote" -> ParagraphBlock(block, alignModifier, textAlign)
        "pub.leaflet.blocks.code" -> CodeBlock(block)
        "pub.leaflet.blocks.math" -> MathBlock(block)
        "pub.leaflet.blocks.image" -> ImageBlock(block, authorDid)
        "pub.leaflet.blocks.unorderedList" -> UnorderedListBlock(block, pollData, onLoadPoll, onCastVote)
        "pub.leaflet.blocks.orderedList" -> OrderedListBlock(block, pollData, onLoadPoll, onCastVote)
        "pub.leaflet.blocks.checklist" -> ChecklistBlock(block, pollData, onLoadPoll, onCastVote)
        "pub.leaflet.blocks.bskyPost" -> BskyPostBlock(block)
        "pub.leaflet.blocks.standardSitePost" -> StandardSitePostBlock(block)
        "pub.leaflet.blocks.website" -> WebsiteEmbedBlock(block)
        "pub.leaflet.blocks.iframe" -> IframeEmbedBlock(block)
        "pub.leaflet.blocks.button" -> ButtonBlock(block)
        "pub.leaflet.blocks.divider" -> DividerBlock()
        "pub.leaflet.blocks.page" -> PageBlock(block)
        "pub.leaflet.blocks.postsList" -> PostsListBlock(block)
        "pub.leaflet.blocks.signup" -> SignupBlock()
        "pub.leaflet.blocks.poll" -> PollBlock(block, authorDid, pollData, onLoadPoll, onCastVote)
        else -> UnknownBlock(block)
    }
}

// MARK: - Facet Rendering

fun buildAnnotatedString(text: String, facets: List<LeafletFacet>?): AnnotatedString {
    if (facets.isNullOrEmpty() || text.isEmpty()) return AnnotatedString(text)

    val builder = AnnotatedString.Builder(text)

    for (facet in facets) {
        val range = byteOffsetsToCharRange(text, facet.index.byteStart, facet.index.byteEnd) ?: continue

        for (feature in facet.features) {
            when (feature.type) {
                "pub.leaflet.richtext.facet#bold",
                "blog.pckt.richtext.facet#bold",
                "app.offprint.richtext.facet#bold" -> {
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.start, range.endInclusive + 1)
                }
                "pub.leaflet.richtext.facet#italic",
                "blog.pckt.richtext.facet#italic",
                "app.offprint.richtext.facet#italic" -> {
                    builder.addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), range.start, range.endInclusive + 1)
                }
                "pub.leaflet.richtext.facet#code",
                "blog.pckt.richtext.facet#code",
                "app.offprint.richtext.facet#code" -> {
                    builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), range.start, range.endInclusive + 1)
                }
                "pub.leaflet.richtext.facet#strikethrough",
                "blog.pckt.richtext.facet#strikethrough",
                "app.offprint.richtext.facet#strikethrough" -> {
                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.start, range.endInclusive + 1)
                }
                "pub.leaflet.richtext.facet#link",
                "blog.pckt.richtext.facet#link",
                "app.offprint.richtext.facet#link" -> {
                    feature.uri?.let { uri ->
                        builder.addStringAnnotation(tag = "URL", annotation = uri, start = range.start, end = range.endInclusive + 1)
                    }
                }
            }
        }
    }

    return builder.toAnnotatedString()
}

private fun byteOffsetsToCharRange(text: String, byteStart: Int, byteEnd: Int): IntRange? {
    if (byteEnd <= byteStart || byteStart < 0) return null

    var startChar = -1
    var endChar = -1
    var bytePos = 0

    for (i in text.indices) {
        val c = text[i]
        val charBytes = when {
            c.code < 0x80 -> 1
            c.code < 0x800 -> 2
            c.code < 0xD800 || c.code > 0xDFFF -> 3
            else -> 4
        }

        if (startChar == -1 && bytePos + charBytes > byteStart) {
            startChar = i
        }
        if (endChar == -1 && bytePos + charBytes > byteEnd) {
            endChar = i
        }

        bytePos += charBytes
        if (startChar != -1 && endChar != -1) break
    }

    if (startChar == -1) startChar = text.length
    if (endChar == -1) endChar = text.length
    if (bytePos < byteEnd) endChar = text.length

    if (startChar >= endChar) return null
    return startChar..endChar
}

@Composable
private fun FacetedText(
    text: String,
    facets: List<LeafletFacet>?,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    onLinkClick: ((String, android.content.Context) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotated = buildAnnotatedString(text, facets)
    val hasLinks = annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (hasLinks && onLinkClick != null) {
        androidx.compose.foundation.text.ClickableText(
            text = annotated,
            style = style.copy(textAlign = textAlign),
            modifier = modifier,
            onClick = { offset ->
                val links = annotated.getStringAnnotations("URL", 0, annotated.length)
                links.firstOrNull { offset >= it.start && offset < it.end }
                    ?.let { onLinkClick(it.item, context) }
            }
        )
    } else {
        Text(
            text = annotated,
            style = style.copy(textAlign = textAlign),
            modifier = modifier,
            maxLines = maxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// MARK: - Block Renderers

@Composable
fun TextBlock(block: LeafletBlock, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    FacetedText(
        text = block.plaintext ?: "",
        facets = block.facets,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
        onLinkClick = { url, ctx -> openUrl(ctx, url) }
    )
}

@Composable
fun HeaderBlock(block: LeafletBlock, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    val level = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.headlineSmall
        3 -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleLarge
    }
    FacetedText(
        text = block.plaintext ?: "",
        facets = block.facets,
        style = level,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
        onLinkClick = { url, ctx -> openUrl(ctx, url) }
    )
}

@Composable
fun ParagraphBlock(block: LeafletBlock, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    if (block.type == "pub.leaflet.blocks.blockquote") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.width(4.dp).height(40.dp).background(MaterialTheme.colorScheme.primary)
            )
            Spacer(Modifier.width(8.dp))
            FacetedText(
                text = block.plaintext ?: "",
                facets = block.facets,
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                textAlign = textAlign,
                onLinkClick = { url, ctx -> openUrl(ctx, url) }
            )
        }
    } else {
        FacetedText(
            text = block.plaintext ?: "",
            facets = block.facets,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.fillMaxWidth(),
            textAlign = textAlign,
            onLinkClick = { url, ctx -> openUrl(ctx, url) }
        )
    }
}

@Composable
fun CodeBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            val language = block.language?.let { " | $it" } ?: ""
            Text(
                "Code$language",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                block.plaintext ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MathBlock(block: LeafletBlock) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "TeX",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                block.tex ?: "",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ImageBlock(block: LeafletBlock, authorDid: String) {
    val imageUrl = if (block.image?.link?.startsWith("http") == true) block.image.link else "https://cdn.bsky.app/img/feed_thumbnail/plain/${authorDid}/${block.image?.link}"
    AsyncImage(
        model = imageUrl,
        contentDescription = block.alt,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        contentScale = ContentScale.Crop
    )
}

@Composable
fun UnorderedListBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ListItem(item, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun OrderedListBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEachIndexed { index, item -> ListItem(item, index + 1, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun ChecklistBlock(
    block: LeafletBlock,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ChecklistItem(item, pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote) }
    }
}

@Composable
fun ListItem(
    item: ListItemModel,
    number: Int? = null,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (item.type == "pub.leaflet.blocks.checklist") {
            Box(
                modifier = Modifier.size(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (item.checked == true) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank
                val tint = if (item.checked == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
            }
            Spacer(Modifier.width(12.dp))
        } else if (number != null) {
            Text(
                "$number.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.width(24.dp)
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Icon(
                Icons.Outlined.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
        }
        if (item.content != null) {
            LeafletBlockContent(item.content, "", Modifier.weight(1f), pollData = pollData, onLoadPoll = onLoadPoll, onCastVote = onCastVote)
        }
    }
}

@Composable
fun ChecklistItem(
    item: ListItemModel,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap()),
    onLoadPoll: suspend (StrongRef) -> Unit = {},
    onCastVote: suspend (String, List<String>) -> Unit = { _, _ -> },
) {
    ListItem(item)
}

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
            Text(formatCount(count), style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> "$count"
    }
}

@Composable
fun StandardSitePostBlock(block: LeafletBlock) {
    val subjectUri = block.standardSitePostSubject ?: block.subject?.uri ?: return
    val size = block.size

    var document by remember { mutableStateOf<StandardSitePostData?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(subjectUri) {
        val result = fetchStandardSitePost(subjectUri)
        if (result != null) {
            document = result
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
                    Text("Loading...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            loadError -> {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Standard.site post unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            document != null -> {
                StandardSitePostContent(doc = document!!, isSmall = size == "small")
            }
        }
    }
}

private data class StandardSitePostData(
    val title: String,
    val description: String?,
    val publishedAt: String?,
    val coverImageCid: String?,
    val authorDid: String,
    val publicationName: String?,
)

private suspend fun fetchStandardSitePost(uri: String): StandardSitePostData? {
    return try {
        val parsed = uk.ewancroft.inkwell.data.model.common.AtUri.parse(uri) ?: return null
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        // Fetch the document record
        val docUrl = "https://public.api.bsky.app/xrpc/com.atproto.repo.getRecord?repo=${parsed.did}&collection=${parsed.collection}&rkey=${parsed.recordKey}"
        val docRequest = okhttp3.Request.Builder().url(docUrl).build()
        val docResponse = client.newCall(docRequest).execute()
        if (!docResponse.isSuccessful) return null

        val docBody = docResponse.body?.string() ?: return null
        val docJson = kotlinx.serialization.json.Json.parseToJsonElement(docBody).jsonObject
        val value = docJson["value"]?.jsonObject ?: return null

        val title = value["title"]?.jsonPrimitive?.contentOrNull ?: return null
        val description = value["description"]?.jsonPrimitive?.contentOrNull
        val publishedAt = value["publishedAt"]?.jsonPrimitive?.contentOrNull
        val coverImage = value["coverImage"]?.jsonObject?.get("\$link")?.jsonPrimitive?.contentOrNull

        // Best-effort publication fetch for context
        var publicationName: String? = null
        val siteUri = value["site"]?.jsonPrimitive?.contentOrNull
        if (siteUri != null && siteUri.startsWith("at://")) {
            val pubParsed = uk.ewancroft.inkwell.data.model.common.AtUri.parse(siteUri)
            if (pubParsed != null) {
                val pubUrl = "https://public.api.bsky.app/xrpc/com.atproto.repo.getRecord?repo=${pubParsed.did}&collection=${pubParsed.collection}&rkey=${pubParsed.recordKey}"
                val pubRequest = okhttp3.Request.Builder().url(pubUrl).build()
                val pubResponse = client.newCall(pubRequest).execute()
                if (pubResponse.isSuccessful) {
                    val pubBody = pubResponse.body?.string()
                    if (pubBody != null) {
                        val pubJson = kotlinx.serialization.json.Json.parseToJsonElement(pubBody).jsonObject
                        val pubValue = pubJson["value"]?.jsonObject
                        publicationName = pubValue?.get("name")?.jsonPrimitive?.contentOrNull
                    }
                }
            }
        }

        StandardSitePostData(
            title = title,
            description = description,
            publishedAt = publishedAt,
            coverImageCid = coverImage,
            authorDid = parsed.did,
            publicationName = publicationName,
        )
    } catch (e: Exception) {
        android.util.Log.e("LeafletBlockRenderer", "Error fetching StandardSite post: ${e.message}")
        null
    }
}

@Composable
private fun StandardSitePostContent(doc: StandardSitePostData, isSmall: Boolean) {
    Column {
        // Cover image
        if (!isSmall && doc.coverImageCid != null) {
            AsyncImage(
                model = "https://cdn.bsky.app/img/feed_thumbnail/plain/${doc.authorDid}/${doc.coverImageCid}",
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            Modifier.padding(if (isSmall) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (isSmall) 4.dp else 8.dp),
        ) {
            // Publication name
            if (doc.publicationName != null) {
                Text(
                    doc.publicationName.uppercase(),
                    style = if (isSmall) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    letterSpacing = 1.sp,
                    maxLines = 1,
                )
            }

            // Document title
            Text(
                doc.title,
                style = if (isSmall) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (isSmall) 2 else 3,
            )

            // Description
            if (!isSmall && !doc.description.isNullOrBlank()) {
                Text(
                    doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 2,
                )
            }

            // Published date
            if (doc.publishedAt != null) {
                Text(
                    doc.publishedAt.formatPublishedDate(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
fun WebsiteEmbedBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            if (block.websiteTitle != null) {
                Text(
                    block.websiteTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            if (block.websiteDescription != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    block.websiteDescription,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (block.url != null) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(block.url, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun IframeEmbedBlock(block: LeafletBlock) {
    val url = block.url ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    loadUrl(url)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(block.height?.dp ?: 300.dp)
        )
    }
}

@Composable
fun ButtonBlock(block: LeafletBlock) {
    val url = block.url
    val text = block.text ?: ""
    val context = androidx.compose.ui.platform.LocalContext.current
    if (url != null) {
        OutlinedButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        Button(
            onClick = { /* no-op without URL */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
fun DividerBlock() {
    HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
fun PageBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.AutoMirrored.Outlined.Article, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Page ${block.pageIndex ?: 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PostsListBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Outlined.ViewList, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Posts", style = MaterialTheme.typography.labelMedium)
            }
            if (!block.websiteTitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    block.websiteTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (block.websiteDescription != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    block.websiteDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tags = block.websiteTitle?.split(",")?.map { it.trim() } ?: emptyList()
                if (tags.isEmpty()) {
                    item {
                        AssistChip(onClick = {}, label = { Text("All posts") }, enabled = false)
                    }
                } else {
                    items(tags) { tag ->
                        AssistChip(onClick = {}, label = { Text(tag) }, enabled = false)
                    }
                }
            }
        }
    }
}

@Composable
fun SignupBlock() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Outlined.MailOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Sign-up form — visit the publication's website to subscribe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun UnknownBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "Unsupported: ${block.type}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
private fun PollBlock(
    block: LeafletBlock,
    authorDid: String,
    pollData: kotlinx.coroutines.flow.StateFlow<Map<String, PostDetailViewModel.PollData>>,
    onLoadPoll: suspend (StrongRef) -> Unit,
    onCastVote: suspend (String, List<String>) -> Unit,
) {
    var isLoading by remember { mutableStateOf(true) }
    var selectedOptions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var hasVoted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pollRef = block.poll
    val pollUri = pollRef?.uri ?: ""

    LaunchedEffect(pollUri, authorDid) {
        if (pollUri.isBlank()) return@LaunchedEffect
        isLoading = true
        onLoadPoll(pollRef!!)
        isLoading = false
    }

    val myPollData by pollData.collectAsStateWithLifecycle(initialValue = emptyMap())
    val data: PostDetailViewModel.PollData? = myPollData[pollUri]
    hasVoted = data?.myVote?.isNotEmpty() == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            } else if (data != null) {
                Text(
                    data.definition.name ?: "Poll",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val options = data.definition.options.orEmpty()
                val totalVotes = data.totalVotes
                options.forEach { option ->
                    val count = data.voteCounts[option.text] ?: 0
                    val fraction = if (totalVotes > 0) count.toFloat() / totalVotes else 0f
                    val isSelected = selectedOptions.contains(option.text)
                    val isVoted = hasVoted

                    OutlinedButton(
                        onClick = {
                            if (!isVoted) {
                                val newSelection = if (isSelected) {
                                    selectedOptions - option.text
                                } else {
                                    selectedOptions + option.text
                                }
                                selectedOptions = newSelection
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isVoted,
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (totalVotes > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = if (isVoted) 0.15f else 0.08f),
                                            MaterialTheme.shapes.small
                                        )
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    option.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (totalVotes > 0) {
                                        Text(
                                            "$count",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isVoted && isSelected) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (!hasVoted && selectedOptions.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val selected = selectedOptions.toList()
                            hasVoted = true
                            selectedOptions = emptySet()
                            scope.launch {
                                onCastVote(pollUri, selected)
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Vote")
                    }
                }
                if (totalVotes > 0) {
                    Text(
                        "$totalVotes vote${if (totalVotes == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    "Poll",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
