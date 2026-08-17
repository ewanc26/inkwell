package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for an AT Protocol strong reference (URI + CID).
 *
 * Mirrors Android `StrongRef` and iOS `ComAtprotoLexicon.Repository.StrongReference`.
 */
data class StrongRef(
    val uri: String,
    val cid: String? = null
)
