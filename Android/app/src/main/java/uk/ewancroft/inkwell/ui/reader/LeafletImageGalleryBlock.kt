package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uk.ewancroft.inkwell.data.model.content.LeafletBlock
import uk.ewancroft.inkwell.data.model.content.LeafletGalleryImage

/**
 * A `pub.leaflet.blocks.imageGallery` block. Images are PDS blobs, loaded
 * through the same callback as single-image blocks; the lexicon's gallery
 * formats all show the same ordered set, so they render as one horizontal
 * strip with each image's own aspect ratio.
 */
@Composable
fun ImageGalleryBlock(
    block: LeafletBlock,
    authorDid: String,
    onLoadImage: suspend (String, String) -> ByteArray?,
) {
    val images = block.images.orEmpty().filter { it.image != null }
    if (images.isEmpty()) return

    val gap = (block.gap ?: 8).coerceIn(0, 32).dp
    val maxTileWidth = (block.maxWidth ?: 320).coerceIn(140, 480)

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(gap),
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        items(images, key = { it.image?.link ?: it.hashCode().toString() }) { item ->
            GalleryTile(item, authorDid, maxTileWidth, onLoadImage)
        }
    }
}

@Composable
private fun GalleryTile(
    item: LeafletGalleryImage,
    authorDid: String,
    maxTileWidth: Int,
    onLoadImage: suspend (String, String) -> ByteArray?,
) {
    val link = item.image?.link ?: return
    val bytes by produceState<ByteArray?>(initialValue = null, authorDid, link) {
        value = onLoadImage(authorDid, link)
    }
    val width = galleryTileWidth(item, maxTileWidth)

    Column(Modifier.width(width)) {
        AsyncImage(
            model = bytes,
            contentDescription = item.alt,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        if (!item.alt.isNullOrBlank()) {
            Text(
                item.alt,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

private fun galleryTileWidth(item: LeafletGalleryImage, maxTileWidth: Int) = run {
    val ratio = item.aspectRatio
    val width = ratio?.width
    val height = ratio?.height
    if (width != null && height != null && width > 0 && height > 0) {
        (220f * width / height).coerceIn(140f, maxTileWidth.toFloat()).dp
    } else {
        240.dp
    }
}
