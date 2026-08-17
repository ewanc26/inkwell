package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a `site.standard.publication` record.
 *
 * Mirrors Android `PublicationRecord` and iOS `PublicationRecord`.
 * Only the fields needed by shared KMP logic and cross-platform mapping
 * are included here.
 */
data class SharedPublicationRecord(
    val type: String = "site.standard.publication",
    val url: String,
    val name: String,
    val description: String? = null,
    val icon: BlobRef? = null,
    val theme: PublicationTheme? = null,
    val basicTheme: BasicTheme? = null,
    val preferences: PublicationPreferences? = null
)
