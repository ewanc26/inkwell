package uk.ewancroft.inkwell.ui.writer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * A horizontal toolbar of markdown formatting buttons for the writer.
 * Each button inserts markdown syntax at the editor's actual cursor (or
 * wraps the current selection) rather than always appending to the end of
 * the document.
 *
 * Matches the formatting toolbar in standard.horse's PostEditor.
 */
@Composable
fun FormattingToolbar(
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    canUploadImages: Boolean,
    onImagePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    /** Wraps the selection in [before]/[after] (e.g. `**bold**`). With no
     * selection, inserts an empty pair with the cursor left between them. */
    fun wrapSelection(before: String, after: String) {
        val range = textFieldValue.selection
        val text = textFieldValue.text
        val selected = text.substring(range.min, range.max)
        val replacement = "$before$selected$after"
        val newText = text.replaceRange(range.min, range.max, replacement)
        val cursor = range.min + if (selected.isEmpty()) before.length else replacement.length
        onTextFieldValueChange(TextFieldValue(newText, TextRange(cursor)))
    }

    /** Inserts [prefix] at the start of the line containing the cursor
     * (e.g. `## ` for a heading) rather than always starting a new line at
     * the end of the document. */
    fun prependToLine(prefix: String) {
        val text = textFieldValue.text
        val cursorPos = textFieldValue.selection.min
        val lineStart = text.lastIndexOf('\n', (cursorPos - 1).coerceAtLeast(0)).let {
            if (it == -1) 0 else it + 1
        }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(lineStart + prefix.length)))
    }

    /** Inserts literal text at the cursor, replacing any selection. */
    fun insertText(text: String) {
        val range = textFieldValue.selection
        val newText = textFieldValue.text.replaceRange(range.min, range.max, text)
        onTextFieldValueChange(TextFieldValue(newText, TextRange(range.min + text.length)))
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatButton(icon = Icons.Outlined.FormatBold, label = "Bold") { wrapSelection("**", "**") }
        FormatButton(icon = Icons.Outlined.FormatItalic, label = "Italic") { wrapSelection("*", "*") }
        FormatButton(icon = Icons.Outlined.Title, label = "Heading") { prependToLine("## ") }
        FormatButton(icon = Icons.Outlined.FormatQuote, label = "Quote") { prependToLine("> ") }
        FormatButton(icon = Icons.Outlined.Code, label = "Code") { wrapSelection("`", "`") }
        FormatButton(icon = Icons.Outlined.Link, label = "Link") { insertText("[text](url)") }
        FormatButton(
            icon = Icons.Outlined.Image,
            label = "Image",
            enabled = canUploadImages,
            onClick = onImagePicker,
        )
    }
}

@Composable
private fun FormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
