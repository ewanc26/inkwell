package uk.ewancroft.inkwell.shared.model

/**
 * Neutral shared model for a Leaflet facet feature.
 *
 * Mirrors Android `FacetFeature` and iOS `LeafletFacetFeature`.
 */
data class LeafletFacetFeature(
    val type: String,
    val uri: String? = null,
    val tag: String? = null,
    val did: String? = null
)
