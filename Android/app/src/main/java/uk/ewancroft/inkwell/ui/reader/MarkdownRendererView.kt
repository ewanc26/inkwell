package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.shared.markdown.InlineMarkdownScanner
import uk.ewancroft.inkwell.shared.markdown.InlineSegment
import uk.ewancroft.inkwell.util.AccessibilityPreferences

/**
 * A Compose view that renders markdown text using the same MarkdownParser
 * the writer uses. Mirrors the iOS MarkdownRendererView.swift.
 *
 * Reads AccessibilityPreferences directly (font size scale, bold text,
 * underline links) rather than threading them through every call site,
 * mirroring how iOS's ReaderTheme/MarkdownRendererView read
 * AccessibilitySettings.shared directly. Font size scale is applied via
 * a LocalDensity override, the same mechanism Android's own system font
 * scale setting uses, so it composes with -- rather than fights -- the
 * system setting and every `.sp` size in this file uniformly.
 */
@Composable
fun MarkdownRendererView(
    markdown: String,
    foregroundColor: Color = Color.Unspecified,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val context = LocalContext.current
    val fontSizeScale = AccessibilityPreferences.getFontSizeScale(context)
    val boldText = AccessibilityPreferences.getBoldText(context)
    val underlineLinks = AccessibilityPreferences.getUnderlineLinks(context)

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, fontSizeScale) {
        Density(baseDensity.density, baseDensity.fontScale * fontSizeScale)
    }

    fun TextStyle.maybeBold(): TextStyle = if (boldText) copy(fontWeight = FontWeight.Bold) else this

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
    val blocks = remember(markdown) { MarkdownParser.parse(markdown) }
    val bodyColor = if (foregroundColor != Color.Unspecified) foregroundColor else MaterialTheme.colorScheme.onBackground
    val mutedColor = bodyColor.copy(alpha = 0.6f)
    val surfaceColor = bodyColor.copy(alpha = 0.04f)
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    val style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        3 -> MaterialTheme.typography.titleMedium
                        4 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.labelLarge
                    }
                    Text(
                        text = renderInline(block.text, bodyColor, accentColor, uriHandler, underlineLinks),
                        style = style,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (block.level == 1) 8.dp else 4.dp),
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderInline(block.text, bodyColor, accentColor, uriHandler, underlineLinks),
                        style = MaterialTheme.typography.bodyLarge.maybeBold(),
                        lineHeight = 24.sp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                is MarkdownBlock.Code -> {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        if (!block.language.isNullOrEmpty()) {
                            Text(
                                text = block.language.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = mutedColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bodyColor.copy(alpha = 0.06f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            text = block.content,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = bodyColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceColor)
                                .padding(12.dp),
                        )
                    }
                }

                is MarkdownBlock.Math -> {
                    Text(
                        text = "\$\$ ${block.tex} \$\$",
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
                        color = bodyColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(surfaceColor)
                            .padding(12.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                is MarkdownBlock.Blockquote -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    ) {
                        HorizontalDivider(
                            color = accentColor,
                            modifier = Modifier
                                .padding(start = 0.dp)
                                .height(2.dp),
                        )
                        Text(
                            text = renderInline(block.text, bodyColor, accentColor, uriHandler, underlineLinks),
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic).maybeBold(),
                            color = mutedColor,
                            lineHeight = 24.sp,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 8.dp)
                                .fillMaxWidth(),
                        )
                    }
                }

                is MarkdownBlock.Image -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = block.url,
                            contentDescription = block.alt.ifEmpty { null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .heightIn(min = 180.dp, max = 400.dp),
                        )
                        if (block.alt.isNotEmpty()) {
                            Text(
                                text = block.alt,
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic,
                                color = mutedColor,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    HorizontalDivider(
                        color = accentColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }

                is MarkdownBlock.UnorderedList -> {
                    renderListItems(block.items, ordered = false, startIndex = null, bodyColor, accentColor, uriHandler, boldText, underlineLinks)
                }

                is MarkdownBlock.OrderedList -> {
                    renderListItems(block.items, ordered = true, startIndex = block.start, bodyColor, accentColor, uriHandler, boldText, underlineLinks)
                }

                is MarkdownBlock.TaskList -> {
                    renderTaskList(block.items, bodyColor, accentColor, uriHandler, boldText, underlineLinks)
                }
            }
        }
    }
    }
}

@Composable
private fun renderListItems(
    items: List<MarkdownListItem>,
    ordered: Boolean,
    startIndex: Int?,
    bodyColor: Color,
    accentColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    boldText: Boolean = false,
    underlineLinks: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
    ) {
        for ((index, item) in items.withIndex()) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                if (ordered) {
                    val num = (startIndex ?: 1) + index
                    Text(
                        text = "$num.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = accentColor,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.titleMedium,
                        color = accentColor,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = renderInline(item.text, bodyColor, accentColor, uriHandler, underlineLinks),
                        style = if (boldText) {
                            MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = bodyColor,
                    )
                    if (!item.children.isNullOrEmpty()) {
                        renderListItems(item.children, ordered = false, startIndex = null, bodyColor, accentColor, uriHandler, boldText, underlineLinks)
                    }
                }
            }
        }
    }
}

@Composable
private fun renderTaskList(
    items: List<MarkdownListItem>,
    bodyColor: Color,
    accentColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    boldText: Boolean = false,
    underlineLinks: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
    ) {
        for (item in items) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                val checkChar = if (item.checked == true) "\u2611" else "\u2610"
                val checkColor = if (item.checked == true) accentColor else bodyColor.copy(alpha = 0.5f)
                Text(
                    text = checkChar,
                    style = MaterialTheme.typography.bodyLarge,
                    color = checkColor,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = renderInline(item.text, bodyColor, accentColor, uriHandler, underlineLinks),
                    style = if (boldText) {
                        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    } else {
                        MaterialTheme.typography.bodyLarge
                    },
                    color = bodyColor,
                    textDecoration = if (item.checked == true) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Renders markdown inline syntax (**bold**, *italic*, `code`,
 * ~~strike~~, [text](url)) as an AnnotatedString.
 *
 * Delegates scanning to [InlineMarkdownScanner] and maps the resulting
 * segments to Compose [SpanStyle]s.
 */
private fun renderInline(
    text: String,
    bodyColor: Color,
    accentColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    underlineLinks: Boolean = true,
): AnnotatedString {
    val segments = InlineMarkdownScanner.scan(text)
    return buildAnnotatedString {
        for (segment in segments) {
            when (segment) {
                is InlineSegment.Plain -> append(segment.text)
                is InlineSegment.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = bodyColor)) {
                    append(segment.text)
                }
                is InlineSegment.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = bodyColor)) {
                    append(segment.text)
                }
                is InlineSegment.Code -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = bodyColor)) {
                    append(segment.text)
                }
                is InlineSegment.Strike -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = bodyColor)) {
                    append(segment.text)
                }
                is InlineSegment.Link -> {
                    pushStringAnnotation(tag = "URL", annotation = segment.url)
                    val decoration = if (underlineLinks) TextDecoration.Underline else TextDecoration.None
                    withStyle(SpanStyle(color = accentColor, textDecoration = decoration)) {
                        append(segment.text)
                    }
                    pop()
                }
            }
        }
    }
}
