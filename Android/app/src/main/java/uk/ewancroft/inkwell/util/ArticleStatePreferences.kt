package uk.ewancroft.inkwell.util

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local (device-only) read/bookmark tracking, keyed by document AT-URI.
 * Context-parameterised to match CustomisationPreferences/TipPromptManager's
 * convention for lightweight preference reads outside the DI graph -- a
 * Room database felt like overkill for two booleans per article. Mirrors
 * iOS ArticleStateStore.swift.
 */
object ArticleStatePreferences {
    private const val PREFS_NAME = "inkwell_article_state"
    private const val STATE_KEY = "article_state"

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class ArticleState(
        val title: String,
        val isRead: Boolean = false,
        val isBookmarked: Boolean = false,
        val updatedAt: Long = System.currentTimeMillis(),
    )

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun readAll(context: Context): Map<String, ArticleState> {
        val raw = prefs(context).getString(STATE_KEY, null) ?: return emptyMap()
        return runCatching { json.decodeFromString<Map<String, ArticleState>>(raw) }.getOrDefault(emptyMap())
    }

    private fun writeAll(context: Context, states: Map<String, ArticleState>) {
        prefs(context).edit().putString(STATE_KEY, json.encodeToString(states)).apply()
    }

    fun isRead(context: Context, articleId: String): Boolean = readAll(context)[articleId]?.isRead ?: false

    fun isBookmarked(context: Context, articleId: String): Boolean = readAll(context)[articleId]?.isBookmarked ?: false

    /** Called when a document is opened. A no-op once already marked. */
    fun markAsRead(context: Context, articleId: String, title: String) {
        val states = readAll(context)
        if (states[articleId]?.isRead == true) return
        val updated = (states[articleId] ?: ArticleState(title = title)).copy(
            title = title,
            isRead = true,
            updatedAt = System.currentTimeMillis(),
        )
        writeAll(context, states + (articleId to updated))
    }

    fun setBookmarked(context: Context, articleId: String, title: String, bookmarked: Boolean) {
        val states = readAll(context)
        val updated = (states[articleId] ?: ArticleState(title = title)).copy(
            title = title,
            isBookmarked = bookmarked,
            updatedAt = System.currentTimeMillis(),
        )
        writeAll(context, states + (articleId to updated))
    }

    @Serializable
    private data class ExportedArticleState(
        val articleId: String,
        val title: String,
        val isRead: Boolean,
        val isBookmarked: Boolean,
        val timestamp: Long,
    )

    /** Pretty-printed JSON array for the Settings -> Export Data action. */
    fun exportJson(context: Context): String {
        val items = readAll(context).map { (id, state) ->
            ExportedArticleState(
                articleId = id,
                title = state.title,
                isRead = state.isRead,
                isBookmarked = state.isBookmarked,
                timestamp = state.updatedAt,
            )
        }.sortedByDescending { it.timestamp }

        val prettyJson = Json { prettyPrint = true }
        return prettyJson.encodeToString(items)
    }
}
