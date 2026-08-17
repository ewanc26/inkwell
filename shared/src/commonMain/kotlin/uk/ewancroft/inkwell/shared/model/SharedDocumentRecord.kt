package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a `site.standard.document` record.
 *
 * Mirrors Android `DocumentRecord` and iOS `DocumentRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
data class SharedDocumentRecord(
    val type: String = "site.standard.document",
    val site: String,
    val title: String,
    val publishedAt: String,
    val path: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val textContent: String? = null,
    val coverImage: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val preferences: DocumentPreferences? = null,
    val bskyPostRef: StrongRef? = null
)
