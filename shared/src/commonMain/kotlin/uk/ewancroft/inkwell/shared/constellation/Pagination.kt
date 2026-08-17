package uk.ewancroft.inkwell.shared.constellation

data class ConstellationBacklink(
    val did: String,
    val collection: String,
    val rkey: String
) {
    val recordURI: String
        get() = "at://$did/$collection/$rkey"
}

data class ConstellationResponse(
    val records: List<ConstellationBacklink> = emptyList(),
    val cursor: String? = null
)

object ConstellationPagination {

    private const val DEFAULT_MAX_COUNT = 200
    private const val PAGE_LIMIT = 50

    /**
     * Generic cursor-based pagination over a backlink source.
     */
    suspend fun paginateBacklinks(
        fetchPage: suspend (limit: Int, cursor: String?) -> ConstellationResponse,
        maxCount: Int = DEFAULT_MAX_COUNT
    ): List<ConstellationBacklink> {
        val all = mutableListOf<ConstellationBacklink>()
        var cursor: String? = null
        while (all.size < maxCount) {
            val result = fetchPage(PAGE_LIMIT, cursor)
            all.addAll(result.records)
            cursor = result.cursor
            if (cursor == null) break
        }
        return all.take(maxCount)
    }

    /**
     * Total recommend count for a document, across the whole network.
     *
     * Requests a single record first: if there's no next cursor the whole
     * result set fit in that page, so its size is the count. Otherwise
     * falls back to full pagination.
     */
    suspend fun recommendCount(
        fetchPage: suspend (limit: Int, cursor: String?) -> ConstellationResponse
    ): Int {
        val first = try {
            fetchPage(1, null)
        } catch (_: Exception) {
            return 0
        }
        if (first.cursor == null) return first.records.size
        return paginateBacklinks(fetchPage).size
    }

    /**
     * Deduplicates backlinks by (did, rkey) since a single record could
     * appear in multiple source paths.
     */
    fun deduplicate(backlinks: List<ConstellationBacklink>): List<ConstellationBacklink> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<ConstellationBacklink>()
        for (link in backlinks) {
            val key = "${link.did}:${link.rkey}"
            if (seen.add(key)) {
                result.add(link)
            }
        }
        return result
    }
}
