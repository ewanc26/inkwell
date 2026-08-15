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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.common.BlobRef
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.data.model.content.LeafletFacet
import uk.ewancroft.inkwell.data.model.content.ListItem as ListItemModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafletBlockContent(
    block: LeafletBlock,
    authorDid: String,
    modifier: Modifier = Modifier,
) {
    when (block.type) {
        "pub.leaflet.blocks.text" -> TextBlock(block, modifier)
        "pub.leaflet.blocks.header" -> HeaderBlock(block, modifier)
        "pub.leaflet.blocks.paragraph", "pub.leaflet.blocks.blockquote" -> ParagraphBlock(block, modifier)
        "pub.leaflet.blocks.code" -> CodeBlock(block)
        "pub.leaflet.blocks.math" -> MathBlock(block)
        "pub.leaflet.blocks.image" -> ImageBlock(block, authorDid)
        "pub.leaflet.blocks.unorderedList" -> UnorderedListBlock(block)
        "pub.leaflet.blocks.orderedList" -> OrderedListBlock(block)
        "pub.leaflet.blocks.checklist" -> ChecklistBlock(block)
        "pub.leaflet.blocks.bskyPost" -> BskyPostBlock(block)
        "pub.leaflet.blocks.standardSitePost" -> StandardSitePostBlock(block)
        "pub.leaflet.blocks.website" -> WebsiteEmbedBlock(block)
        "pub.leaflet.blocks.iframe" -> IframeEmbedBlock(block)
        "pub.leaflet.blocks.button" -> ButtonBlock(block)
        "pub.leaflet.blocks.divider" -> DividerBlock()
        "pub.leaflet.blocks.page" -> PageBlock(block)
        "pub.leaflet.blocks.postsList" -> PostsListBlock(block)
        "pub.leaflet.blocks.signup" -> SignupBlock()
        else -> UnknownBlock(block)
    }
}

// MARK: - Facet Rendering

private fun buildAnnotatedString(text: String, facets: List<LeafletFacet>?): AnnotatedString {
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
    onLinkClick: ((String, android.content.Context) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val annotated = buildAnnotatedString(text, facets)
    val hasLinks = annotated.getStringAnnotations("URL", 0, annotated.length).isNotEmpty()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (hasLinks && onLinkClick != null) {
        androidx.compose.foundation.text.ClickableText(
            text = annotated,
            style = style,
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
            style = style,
            modifier = modifier,
            maxLines = maxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

// MARK: - Block Renderers

@Composable
fun TextBlock(block: LeafletBlock, modifier: Modifier = Modifier) {
    FacetedText(
        text = block.plaintext ?: "",
        facets = block.facets,
        style = MaterialTheme.typography.bodyLarge,
        modifier = modifier.fillMaxWidth(),
        onLinkClick = { url, ctx -> openUrl(ctx, url) }
    )
}

@Composable
fun HeaderBlock(block: LeafletBlock, modifier: Modifier = Modifier) {
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
        onLinkClick = { url, ctx -> openUrl(ctx, url) }
    )
}

@Composable
fun ParagraphBlock(block: LeafletBlock, modifier: Modifier = Modifier) {
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
                onLinkClick = { url, ctx -> openUrl(ctx, url) }
            )
        }
    } else {
        FacetedText(
            text = block.plaintext ?: "",
            facets = block.facets,
            style = MaterialTheme.typography.bodyLarge,
            modifier = modifier.fillMaxWidth(),
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
    Text(
        block.tex ?: "",
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        ),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primary
    )
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
fun UnorderedListBlock(block: LeafletBlock) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ListItem(item) }
    }
}

@Composable
fun OrderedListBlock(block: LeafletBlock) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEachIndexed { index, item -> ListItem(item, index + 1) }
    }
}

@Composable
fun ChecklistBlock(block: LeafletBlock) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.children?.forEach { item -> ChecklistItem(item) }
    }
}

@Composable
fun ListItem(item: ListItemModel, number: Int? = null) {
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
            LeafletBlockContent(item.content, "", Modifier.weight(1f))
        }
    }
}

@Composable
fun ChecklistItem(item: ListItemModel) {
    ListItem(item)
}

@Composable
fun BskyPostBlock(block: LeafletBlock) {
    val subject = block.subject
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FiberManualRecord, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bluesky Post", style = MaterialTheme.typography.labelMedium)
                if (subject != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subject.uri,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StandardSitePostBlock(block: LeafletBlock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Standard.site Post", style = MaterialTheme.typography.labelMedium)
            }
            if (block.standardSitePostSubject != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    block.standardSitePostSubject,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Spacer(modifier = Modifier.height(8.dp))
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
