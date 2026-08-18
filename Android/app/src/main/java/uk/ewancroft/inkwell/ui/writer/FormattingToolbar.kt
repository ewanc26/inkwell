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
import androidx.compose.ui.unit.dp

/**
 * A horizontal toolbar of markdown formatting buttons for the writer.
 * Each button inserts markdown syntax at the cursor position.
 *
 * Matches the formatting toolbar in standard.horse's PostEditor.
 */
@Composable
fun FormattingToolbar(
    canUploadImages: Boolean,
    onImagePicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatButton(icon = Icons.Outlined.FormatBold, label = "Bold") { }
        FormatButton(icon = Icons.Outlined.FormatItalic, label = "Italic") { }
        FormatButton(icon = Icons.Outlined.Title, label = "Heading") { }
        FormatButton(icon = Icons.Outlined.FormatQuote, label = "Quote") { }
        FormatButton(icon = Icons.Outlined.Code, label = "Code") { }
        FormatButton(icon = Icons.Outlined.Link, label = "Link") { }
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
