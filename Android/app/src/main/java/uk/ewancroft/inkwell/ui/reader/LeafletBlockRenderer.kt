package uk.ewancroft.inkwell.ui.reader

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import uk.ewancroft.inkwell.data.model.common.StrongRef
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.data.model.content.LeafletFacet
import uk.ewancroft.inkwell.shared.content.CdnUrls
import uk.ewancroft.inkwell.shared.content.LeafletTypes
import uk.ewancroft.inkwell.shared.facets.FacetSchema
import uk.ewancroft.inkwell.shared.text.Utf8Offsets

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
        LeafletTypes.BLOCKS_TEXT -> TextBlock(block, alignModifier, textAlign)
        LeafletTypes.BLOCKS_HEADER -> HeaderBlock(block, alignModifier, textAlign)
        LeafletTypes.BLOCKS_PARAGRAPH, LeafletTypes.BLOCKS_BLOCKQUOTE -> ParagraphBlock(block, alignModifier, textAlign)
        LeafletTypes.BLOCKS_CODE -> CodeBlock(block)
        LeafletTypes.BLOCKS_MATH -> MathBlock(block)
        LeafletTypes.BLOCKS_IMAGE -> ImageBlock(block, authorDid)
        LeafletTypes.BLOCKS_UNORDERED_LIST -> UnorderedListBlock(block, pollData, onLoadPoll, onCastVote)
        LeafletTypes.BLOCKS_ORDERED_LIST -> OrderedListBlock(block, pollData, onLoadPoll, onCastVote)
        LeafletTypes.BLOCKS_CHECKLIST -> ChecklistBlock(block, pollData, onLoadPoll, onCastVote)
        LeafletTypes.BLOCKS_BSKY_POST -> BskyPostBlock(block)
        LeafletTypes.BLOCKS_STANDARD_SITE_POST -> StandardSitePostBlock(block)
        LeafletTypes.BLOCKS_WEBSITE -> WebsiteEmbedBlock(block)
        LeafletTypes.BLOCKS_IFRAME -> IframeEmbedBlock(block)
        LeafletTypes.BLOCKS_BUTTON -> ButtonBlock(block)
        LeafletTypes.BLOCKS_DIVIDER -> DividerBlock()
        LeafletTypes.BLOCKS_PAGE -> PageBlock(block)
        LeafletTypes.BLOCKS_POSTS_LIST -> PostsListBlock(block)
        LeafletTypes.BLOCKS_SIGNUP -> SignupBlock()
        LeafletTypes.BLOCKS_POLL -> PollBlock(block, authorDid, pollData, onLoadPoll, onCastVote)
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
                FacetSchema.leaflet.bold,
                FacetSchema.pckt.bold,
                FacetSchema.offprint.bold -> {
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.start, range.endInclusive + 1)
                }
                FacetSchema.leaflet.italic,
                FacetSchema.pckt.italic,
                FacetSchema.offprint.italic -> {
                    builder.addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), range.start, range.endInclusive + 1)
                }
                FacetSchema.leaflet.code,
                FacetSchema.pckt.code,
                FacetSchema.offprint.code -> {
                    builder.addStyle(SpanStyle(fontFamily = FontFamily.Monospace), range.start, range.endInclusive + 1)
                }
                FacetSchema.leaflet.strike,
                FacetSchema.pckt.strike,
                FacetSchema.offprint.strike -> {
                    builder.addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.start, range.endInclusive + 1)
                }
                FacetSchema.leaflet.link,
                FacetSchema.pckt.link,
                FacetSchema.offprint.link -> {
                    feature.uri?.let { uri ->
                        builder.addStringAnnotation(tag = "URL", annotation = uri, start = range.start, end = range.endInclusive + 1)
                    }
                }
            }
        }
    }

    return builder.toAnnotatedString()
}

private fun byteOffsetsToCharRange(text: String, byteStart: Int, byteEnd: Int): IntRange? =
    Utf8Offsets.byteRangeToCharRange(text, byteStart, byteEnd)

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
    if (block.type == LeafletTypes.BLOCKS_BLOCKQUOTE) {
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
    val imageUrl = if (block.image?.link?.startsWith("http") == true) block.image.link else CdnUrls.bskyThumbnail(authorDid, block.image?.link ?: "")
    AsyncImage(
        model = imageUrl,
        contentDescription = block.alt,
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        contentScale = ContentScale.Crop
    )
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
