package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for an AT Protocol blob reference.
 *
 * Mirrors Android `BlobRef` and iOS `ComAtprotoLexicon.Repository.UploadBlobOutput`.
 * The `link` field corresponds to the `$link` key in AT Protocol JSON.
 */
data class BlobRef(
    val link: String,
    val size: Int = 0,
    val type: String = "blob",
    val mimeType: String? = null
)
