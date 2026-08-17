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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * A Compose view that renders markdown text using the same MarkdownParser
 * the writer uses. Mirrors the iOS MarkdownRendererView.swift.
 */
@Composable
fun MarkdownRendererView(
    markdown: String,
    foregroundColor: Color = Color.Unspecified,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
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
                        text = renderInline(block.text, bodyColor, accentColor, uriHandler),
                        style = style,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (block.level == 1) 8.dp else 4.dp),
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = renderInline(block.text, bodyColor, accentColor, uriHandler),
                        style = MaterialTheme.typography.bodyLarge,
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
                            text = renderInline(block.text, bodyColor, accentColor, uriHandler),
                            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
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
                    renderListItems(block.items, ordered = false, startIndex = null, bodyColor, accentColor, uriHandler)
                }

                is MarkdownBlock.OrderedList -> {
                    renderListItems(block.items, ordered = true, startIndex = block.start, bodyColor, accentColor, uriHandler)
                }

                is MarkdownBlock.TaskList -> {
                    renderTaskList(block.items, bodyColor, accentColor, uriHandler)
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
                        text = renderInline(item.text, bodyColor, accentColor, uriHandler),
                        style = MaterialTheme.typography.bodyLarge,
                        color = bodyColor,
                    )
                    if (!item.children.isNullOrEmpty()) {
                        renderListItems(item.children, ordered = false, startIndex = null, bodyColor, accentColor, uriHandler)
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
                    text = renderInline(item.text, bodyColor, accentColor, uriHandler),
                    style = MaterialTheme.typography.bodyLarge,
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
 */
private fun renderInline(
    text: String,
    bodyColor: Color,
    accentColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val chars = text.toCharArray()

        while (i < chars.size) {
            // Bold: **text**
            if (i + 1 < chars.size && chars[i] == '*' && chars[i + 1] == '*') {
                val end = findClosing(chars, i + 2, "**")
                if (end > 0) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = bodyColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Italic: *text*
            if (chars[i] == '*' && (i + 1 >= chars.size || chars[i + 1] != '*')) {
                val end = findClosing(chars, i + 1, "*")
                if (end > 0 && (end == 0 || chars[end - 1] != '*')) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = bodyColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            // Strikethrough: ~~text~~
            if (i + 1 < chars.size && chars[i] == '~' && chars[i + 1] == '~') {
                val end = findClosing(chars, i + 2, "~~")
                if (end > 0) {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = bodyColor)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }

            // Inline code: `text`
            if (chars[i] == '`') {
                val end = findClosing(chars, i + 1, "`")
                if (end > 0) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = bodyColor)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }

            // Link: [text](url)
            if (chars[i] == '[') {
                val closeBracket = text.indexOf(']', i + 1)
                if (closeBracket > 0 && closeBracket + 1 < chars.size && chars[closeBracket + 1] == '(') {
                    val closeParen = text.indexOf(')', closeBracket + 2)
                    if (closeParen > 0) {
                        val linkText = text.substring(i + 1, closeBracket)
                        val url = text.substring(closeBracket + 2, closeParen)
                        pushStringAnnotation(tag = "URL", annotation = url)
                        withStyle(SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline)) {
                            append(linkText)
                        }
                        pop()
                        i = closeParen + 1
                        continue
                    }
                }
            }

            append(chars[i])
            i++
        }
    }
}

private fun findClosing(text: CharArray, start: Int, delimiter: String): Int {
    val delimChars = delimiter.toCharArray()
    var i = start
    while (i <= text.size - delimChars.size) {
        if (text[i] == delimChars[0] && (delimChars.size == 1 || text[i + 1] == delimChars[1])) {
            return i
        }
        i++
    }
    return -1
}
