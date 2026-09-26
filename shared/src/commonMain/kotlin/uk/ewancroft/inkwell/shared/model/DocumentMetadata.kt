package uk.ewancroft.inkwell.shared.model

/**
 * The optional metadata layer of a `site.standard.document` record.
 *
 * Standard.site defines `coverImage`, `bskyPostRef`, `tags`, `links`, `labels`,
 * `contributors`, `publishedAt` and `updatedAt` alongside the content union
 * (https://standard.site/docs/lexicons/document/). Both platforms need to read
 * these for display and write back the subset a user can edit, so the wire
 * shapes and the tolerance rules live here rather than in each client.
 *
 * Two rules matter more than the shapes:
 *
 * 1. **Unmodelled data survives.** `links` is an open union with no concrete
 *    variants published, so entries are kept as their raw maps and only
 *    *presented* through [Link.displayUri]/[Link.title] when those are
 *    recognisable. A writer that re-serializes a document must put back exactly
 *    what it read.
 * 2. **Absent is not empty.** A field the author never set stays absent rather
 *    than becoming `[]` or `{}` — see [applyTo].
 */
data class DocumentMetadata(
    val tags: List<String> = emptyList(),
    val contributors: List<Contributor> = emptyList(),
    val labels: List<String> = emptyList(),
    val coverImage: Map<String, Any?>? = null,
    val bskyPostRef: BskyPostRef? = null,
    val links: List<Link> = emptyList(),
    val publishedAt: String? = null,
    val updatedAt: String? = null,
) {
    /** A participant on the document beyond the record's author. */
    data class Contributor(
        val did: String,
        val role: String? = null,
        val displayName: String? = null,
    )

    /** Strong reference to the Bluesky post carrying the document's discussion. */
    data class BskyPostRef(val uri: String, val cid: String? = null)

    /**
     * One entry of the open `links` union.
     *
     * [raw] is the entry exactly as it appeared on the wire and is what gets
     * written back; the other properties are presentation hints that are null
     * for variants this version does not recognise.
     */
    data class Link(
        val raw: Map<String, Any?>,
        val type: String? = null,
        val displayUri: String? = null,
        val title: String? = null,
    ) {
        /** True when there is nothing to show a reader beyond an opaque `$type`. */
        val isOpaque: Boolean get() = displayUri == null && title == null
    }

    companion object {
        const val CONTRIBUTOR_TYPE: String = "site.standard.document#contributor"
        const val SELF_LABELS_TYPE: String = "com.atproto.label.defs#selfLabels"
        const val SELF_LABEL_TYPE: String = "com.atproto.label.defs#selfLabel"

        /**
         * Keys this object owns. Everything else in a record is left untouched.
         *
         * Public so a platform writer holding a typed record (rather than the
         * `Map` [applyTo] takes) can clear exactly these keys before writing
         * the metadata back, without re-encoding the fields it doesn't own.
         */
        val OWNED_KEYS: List<String> = listOf(
            "tags", "contributors", "labels", "coverImage", "bskyPostRef", "links",
        )

        /**
         * The content-warning values an author can self-apply, in display order.
         *
         * These are the author-applicable values from the Bluesky label
         * vocabulary (`com.atproto.label.defs#labelValue` known values plus
         * Bluesky's `graphic-media`) — the set that the network's own composers
         * offer as self-labels. Other values already on a record are kept by
         * [read]/[applyTo]; this list is only what a Writer offers to add.
         */
        val SELF_LABEL_VALUES: List<String> = listOf("sexual", "nudity", "porn", "graphic-media")

        /** Property names a `links` variant might use for its target URI. */
        private val URI_KEYS = listOf("uri", "url", "href")

        /** Property names a `links` variant might use for its human-readable label. */
        private val TITLE_KEYS = listOf("title", "name", "label", "text")

        /**
         * Reads the metadata layer out of a decoded document record.
         *
         * Every field is optional and every malformed field degrades to its empty
         * value rather than failing the whole read — a record with one bad
         * contributor must still show its tags.
         */
        fun read(record: Map<String, Any?>): DocumentMetadata = DocumentMetadata(
            tags = readTags(record["tags"]),
            contributors = readContributors(record["contributors"]),
            labels = readLabels(record["labels"]),
            coverImage = record["coverImage"] as? Map<String, Any?>,
            bskyPostRef = readBskyPostRef(record["bskyPostRef"]),
            links = readLinks(record["links"]),
            publishedAt = record["publishedAt"] as? String,
            updatedAt = record["updatedAt"] as? String,
        )

        /**
         * Returns [record] with this metadata's fields replaced.
         *
         * An empty collection removes its key instead of writing `[]`: the
         * Lexicon's fields are optional, and an empty array is a different
         * statement from "not set". Keys outside [OWNED_KEYS] pass through
         * untouched so an editor cannot clobber fields it does not model.
         *
         * `publishedAt`/`updatedAt` are deliberately *not* written here — the
         * Writer owns those timestamps and sets them at submission time.
         */
        fun applyTo(record: Map<String, Any?>, metadata: DocumentMetadata): Map<String, Any?> {
            val result = record.toMutableMap()
            OWNED_KEYS.forEach(result::remove)

            metadata.tags.map(String::trim).filter(String::isNotEmpty).distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { result["tags"] = it }

            metadata.contributors.filter { it.did.isNotBlank() }
                .map(::contributorWire)
                .takeIf { it.isNotEmpty() }
                ?.let { result["contributors"] = it }

            labelsWire(metadata.labels)?.let { result["labels"] = it }

            metadata.coverImage?.let { result["coverImage"] = it }

            metadata.bskyPostRef?.takeIf { it.uri.isNotBlank() }
                ?.let { result["bskyPostRef"] = bskyPostRefWire(it) }

            // Written back verbatim: this client recognises no concrete variant,
            // so anything it re-shaped would be data loss.
            metadata.links.map { it.raw }
                .takeIf { it.isNotEmpty() }
                ?.let { result["links"] = it }

            return result
        }

        /** Canonical `site.standard.document#contributor` wire shape. */
        fun contributorWire(contributor: Contributor): Map<String, Any?> = buildMap {
            put("\$type", CONTRIBUTOR_TYPE)
            put("did", contributor.did.trim())
            contributor.role?.trim()?.takeIf(String::isNotEmpty)?.let { put("role", it) }
            contributor.displayName?.trim()?.takeIf(String::isNotEmpty)?.let { put("displayName", it) }
        }

        /** Canonical `com.atproto.label.defs#selfLabels` wire shape, or null when empty. */
        fun labelsWire(values: List<String>): Map<String, Any?>? {
            val cleaned = values.map(String::trim).filter(String::isNotEmpty).distinct()
            if (cleaned.isEmpty()) return null
            return mapOf(
                "\$type" to SELF_LABELS_TYPE,
                "values" to cleaned.map { mapOf("\$type" to SELF_LABEL_TYPE, "val" to it) },
            )
        }

        /** Canonical `com.atproto.repo.strongRef` wire shape. */
        fun bskyPostRefWire(ref: BskyPostRef): Map<String, Any?> = buildMap {
            put("uri", ref.uri.trim())
            ref.cid?.trim()?.takeIf(String::isNotEmpty)?.let { put("cid", it) }
        }

        private fun readTags(value: Any?): List<String> =
            (value as? List<*>).orEmpty().mapNotNull { it as? String }
                .map(String::trim).filter(String::isNotEmpty)

        private fun readContributors(value: Any?): List<Contributor> =
            (value as? List<*>).orEmpty().mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val did = (map["did"] as? String)?.trim()?.takeIf(String::isNotEmpty)
                    ?: return@mapNotNull null
                Contributor(
                    did = did,
                    role = (map["role"] as? String)?.trim()?.takeIf(String::isNotEmpty),
                    displayName = (map["displayName"] as? String)?.trim()?.takeIf(String::isNotEmpty),
                )
            }

        private fun readLabels(value: Any?): List<String> {
            val values = (value as? Map<*, *>)?.get("values") as? List<*> ?: return emptyList()
            return values.mapNotNull { entry ->
                ((entry as? Map<*, *>)?.get("val") as? String)?.trim()?.takeIf(String::isNotEmpty)
            }.distinct()
        }

        private fun readBskyPostRef(value: Any?): BskyPostRef? {
            val map = value as? Map<*, *> ?: return null
            val uri = (map["uri"] as? String)?.trim()?.takeIf(String::isNotEmpty) ?: return null
            return BskyPostRef(uri = uri, cid = (map["cid"] as? String)?.trim()?.takeIf(String::isNotEmpty))
        }

        /**
         * Reads `links`, accepting either a single union object or an array of
         * them — the published Lexicon says "open union" without fixing the
         * arity, so a reader that assumes one shape would drop the other.
         */
        private fun readLinks(value: Any?): List<Link> {
            val entries: List<Any?> = when (value) {
                null -> emptyList()
                is List<*> -> value
                else -> listOf(value)
            }
            return entries.mapNotNull { entry ->
                @Suppress("UNCHECKED_CAST")
                val map = entry as? Map<String, Any?> ?: return@mapNotNull null
                Link(
                    raw = map,
                    type = (map["\$type"] as? String)?.takeIf(String::isNotEmpty),
                    displayUri = URI_KEYS.firstNotNullOfOrNull { key ->
                        (map[key] as? String)?.trim()?.takeIf(String::isNotEmpty)
                    },
                    title = TITLE_KEYS.firstNotNullOfOrNull { key ->
                        (map[key] as? String)?.trim()?.takeIf(String::isNotEmpty)
                    },
                )
            }
        }
    }
}
