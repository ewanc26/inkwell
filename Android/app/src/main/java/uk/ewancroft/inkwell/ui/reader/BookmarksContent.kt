package uk.ewancroft.inkwell.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import uk.ewancroft.inkwell.R
import uk.ewancroft.inkwell.util.ArticleStatePreferences
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Locally bookmarked documents. Bookmark state lives in
 * [ArticleStatePreferences], so this needs no network request of its own;
 * opening an entry hands its AT-URI to the same detail route a deep link uses.
 *
 * The list is re-read on resume as well as on (re)composition, so removing a
 * bookmark on the detail screen is reflected when the reader comes back.
 */
@Composable
internal fun BookmarksContent(onOpenPost: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var bookmarks by remember {
        mutableStateOf(ArticleStatePreferences.bookmarks(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bookmarks = ArticleStatePreferences.bookmarks(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (bookmarks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.reader_bookmarks_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.reader_bookmarks_empty_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(bookmarks, key = { it.articleId }) { bookmark ->
                BookmarkCard(
                    bookmark = bookmark,
                    onOpen = { onOpenPost(bookmark.articleId) },
                    onRemove = {
                        ArticleStatePreferences.setBookmarked(
                            context = context,
                            articleId = bookmark.articleId,
                            title = bookmark.title,
                            bookmarked = false,
                        )
                        bookmarks = ArticleStatePreferences.bookmarks(context)
                    },
                )
            }
        }
    }
}

@Composable
private fun BookmarkCard(
    bookmark: ArticleStatePreferences.BookmarkedArticle,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val savedDate = remember(bookmark.savedAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
            .format(Date(bookmark.savedAt))
    }

    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = bookmark.title.ifBlank { stringResource(R.string.reader_bookmarks_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.reader_bookmark_saved, savedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (bookmark.isRead) {
                        Text(
                            stringResource(R.string.reader_bookmark_read),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Bookmark,
                    contentDescription = stringResource(R.string.reader_remove_bookmark),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
