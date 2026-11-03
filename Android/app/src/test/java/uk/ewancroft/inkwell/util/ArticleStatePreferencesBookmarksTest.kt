package uk.ewancroft.inkwell.util

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Bookmarks feed's read path: which stored articles it returns, in what
 * order, and that reading one does not reshuffle the list.
 *
 * Writes go through the real [ArticleStatePreferences] persistence (the same
 * `SharedPreferences` file the app uses on-device) so a stored-shape regression
 * is caught, not just the in-memory mapping.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ArticleStatePreferencesBookmarksTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val first = "at://did:plc:alice/site.standard.document/first"
    private val second = "at://did:plc:alice/site.standard.document/second"
    private val third = "at://did:plc:alice/site.standard.document/third"

    private fun writeStates(json: String) {
        context.getSharedPreferences("inkwell_article_state", Context.MODE_PRIVATE)
            .edit()
            .putString("article_state", json)
            .commit()
    }

    @Test
    fun `lists only bookmarked articles, most recently saved first`() {
        writeStates(
            """
            {
              "$first": {"title":"First","isRead":false,"isBookmarked":true,"updatedAt":3000,"bookmarkedAt":1000},
              "$second": {"title":"Second","isRead":true,"isBookmarked":true,"updatedAt":1000,"bookmarkedAt":2000},
              "$third": {"title":"Third","isRead":false,"isBookmarked":false,"updatedAt":4000}
            }
            """.trimIndent()
        )

        val bookmarks = ArticleStatePreferences.bookmarks(context)

        assertEquals(listOf(second, first), bookmarks.map { it.articleId })
        assertEquals("Second", bookmarks.first().title)
        assertTrue(bookmarks.first().isRead)
    }

    @Test
    fun `falls back to the last state change when a legacy entry has no bookmark time`() {
        writeStates(
            """
            {
              "$first": {"title":"Legacy","isRead":false,"isBookmarked":true,"updatedAt":5000},
              "$second": {"title":"New","isRead":false,"isBookmarked":true,"updatedAt":1000,"bookmarkedAt":1000}
            }
            """.trimIndent()
        )

        val bookmarks = ArticleStatePreferences.bookmarks(context)

        assertEquals(listOf(first, second), bookmarks.map { it.articleId })
        assertEquals(5000L, bookmarks.first().savedAt)
    }

    @Test
    fun `removing a bookmark drops it from the feed but keeps read state`() {
        writeStates(
            """
            {
              "$first": {"title":"First","isRead":true,"isBookmarked":true,"updatedAt":3000,"bookmarkedAt":1000}
            }
            """.trimIndent()
        )

        ArticleStatePreferences.setBookmarked(context, first, "First", bookmarked = false)

        assertTrue(ArticleStatePreferences.bookmarks(context).isEmpty())
        assertTrue(ArticleStatePreferences.isRead(context, first))
    }

    @Test
    fun `reading a bookmarked article does not move it in the feed`() {
        writeStates(
            """
            {
              "$first": {"title":"Older","isRead":false,"isBookmarked":true,"updatedAt":1000,"bookmarkedAt":1000},
              "$second": {"title":"Newer","isRead":false,"isBookmarked":true,"updatedAt":2000,"bookmarkedAt":2000}
            }
            """.trimIndent()
        )

        ArticleStatePreferences.markAsRead(context, first, "Older")

        assertEquals(
            listOf(second, first),
            ArticleStatePreferences.bookmarks(context).map { it.articleId },
        )
    }
}
